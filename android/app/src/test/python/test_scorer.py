"""
Desktop tests for the on-device scorer. Run from `android/`:

    pip install -r app/src/test/python/requirements.txt
    python -m pytest app/src/test/python
"""

import json
import os
import sys

import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "main", "python"))

import scorer  # noqa: E402


def run(**request):
    request.setdefault("seat_wind", "S")
    request.setdefault("round_wind", "E")
    return json.loads(scorer.score(json.dumps(request)))


def split(notation):
    """`123m55p` -> ["1m", "2m", "3m", "5p", "5p"]"""
    tiles, digits = [], ""
    for ch in notation:
        if ch.isdigit():
            digits += ch
        else:
            tiles += [d + ch for d in digits]
            digits = ""
    return tiles


def yaku(result):
    return {y["name"]: y["han"] for y in result["yaku"]}


# 123m 456m 789p 55p + 23s waiting on 1s/4s, winning on 4s (last tile).
PINFU_HAND = split("123456m78955p234s")
PINFU_WIN = 13


def test_riichi_pinfu_tsumo_non_dealer():
    result = run(tiles=PINFU_HAND, win_index=PINFU_WIN, is_tsumo=True, riichi="riichi")
    assert result["ok"]
    assert yaku(result) == {"Riichi": 1, "Menzen Tsumo": 1, "Pinfu": 1}
    assert (result["han"], result["fu"]) == (3, 20)
    assert result["payment"]["main"] == 1300  # dealer
    assert result["payment"]["additional"] == 700  # each non-dealer
    assert result["payment"]["total"] == 2700


def test_pinfu_ron_with_honba_and_riichi_sticks():
    result = run(tiles=PINFU_HAND, win_index=PINFU_WIN, honba=2, riichi_sticks=1)
    assert yaku(result) == {"Pinfu": 1}
    assert (result["han"], result["fu"]) == (1, 30)
    payment = result["payment"]
    assert (payment["main"], payment["main_bonus"], payment["riichi_sticks"]) == (1000, 600, 1000)
    assert payment["total"] == 2600


def test_dealer_ron_splits_ura_dora_from_dora():
    result = run(
        tiles=PINFU_HAND,
        win_index=PINFU_WIN,
        seat_wind="E",
        riichi="riichi",
        dora_indicators=["9p"],  # dora 1p: none in hand
        ura_dora_indicators=["4p"],  # ura 5p: the pair
    )
    assert result["is_dealer"]
    assert yaku(result) == {"Riichi": 1, "Pinfu": 1, "Ura Dora": 2}
    assert (result["han"], result["fu"]) == (4, 30)
    assert result["payment"]["main"] == 11600


def test_dora_and_ura_dora_both_listed():
    result = run(
        tiles=PINFU_HAND,
        win_index=PINFU_WIN,
        riichi="riichi",
        dora_indicators=["3s"],  # dora 4s: one
        ura_dora_indicators=["4p"],  # ura 5p: two
    )
    assert yaku(result) == {"Riichi": 1, "Pinfu": 1, "Dora": 1, "Ura Dora": 2}


def test_red_five_counts_as_aka_dora():
    hand = split("123456m78905p234s")
    result = run(tiles=hand, win_index=13)
    assert yaku(result) == {"Pinfu": 1, "Aka Dora": 1}


def test_open_tanyao_with_pon():
    hand = split("888p234m567p345s66s")
    result = run(tiles=hand, win_index=5, melds=[{"type": "pon", "indices": [0, 1, 2]}])
    assert result["is_open_hand"]
    assert yaku(result) == {"Tanyao": 1}
    assert (result["han"], result["fu"]) == (1, 30)
    assert result["payment"]["main"] == 1000


def test_open_hand_tsumo_has_no_menzen_tsumo():
    # Mahjong Vision noted "menzen tsumo always awarded"; it must not be on an open hand.
    hand = split("888p234m567p345s66s")
    result = run(tiles=hand, win_index=5, is_tsumo=True, melds=[{"type": "pon", "indices": [0, 1, 2]}])
    assert yaku(result) == {"Tanyao": 1}


def test_closed_kan_rinshan():
    hand = split("7777z234m567p88s345s")
    result = run(
        tiles=hand,
        win_index=14,
        is_tsumo=True,
        is_rinshan=True,
        melds=[{"type": "kan", "indices": [0, 1, 2, 3], "opened": False}],
    )
    assert not result["is_open_hand"]
    assert yaku(result) == {"Menzen Tsumo": 1, "Rinshan Kaihou": 1, "Yakuhai (chun)": 1}
    assert (result["han"], result["fu"]) == (3, 60)
    assert (result["payment"]["main"], result["payment"]["additional"]) == (3900, 2000)


def test_dealer_kokushi_tsumo():
    hand = split("119m19p19s1234567z")
    result = run(tiles=hand, win_index=2, seat_wind="E", is_tsumo=True)  # single wait on 9m
    assert result["is_yakuman"]
    assert result["limit"] == "yakuman"
    assert result["payment"]["main"] == 16000
    assert result["payment"]["total"] == 48000


def test_thirteen_sided_kokushi_is_double_yakuman():
    hand = split("19m19p19s1234567z1m")
    result = run(tiles=hand, win_index=13, is_tsumo=True)
    assert result["limit"] == "2x yakuman"


def test_chiitoitsu():
    hand = split("1155m2299p3388s11z")
    result = run(tiles=hand, win_index=13)
    assert "Chiitoitsu" in yaku(result)
    assert result["fu"] == 25


def test_riichi_with_open_hand_is_rejected():
    hand = split("888p234m567p345s66s")
    result = run(tiles=hand, win_index=5, riichi="riichi", melds=[{"type": "pon", "indices": [0, 1, 2]}])
    assert result == {"ok": False, "error": "You can't riichi with an open hand."}


def test_no_yaku():
    hand = split("111m234m567p345s66s")
    result = run(tiles=hand, win_index=5, melds=[{"type": "pon", "indices": [0, 1, 2]}])
    assert not result["ok"]
    assert "no yaku" in result["error"]


def test_not_a_winning_hand():
    hand = split("123456m78955p239s")
    result = run(tiles=hand, win_index=13)
    assert result == {"ok": False, "error": "This isn't a complete winning hand."}


def test_winning_tile_inside_meld_is_rejected():
    hand = split("888p234m567p345s66s")
    result = run(tiles=hand, win_index=0, melds=[{"type": "pon", "indices": [0, 1, 2]}])
    assert not result["ok"]
    assert "called meld" in result["error"]


def test_two_red_fives_of_one_suit_are_rejected():
    hand = split("123456m78900p234s")
    assert run(tiles=hand, win_index=13)["error"] == "There is only one red five per suit."


def test_five_copies_are_rejected():
    hand = split("11111m")
    assert run(tiles=hand, win_index=0)["error"] == "Too many copies of 1m."


def test_ura_dora_without_riichi_is_rejected():
    result = run(tiles=PINFU_HAND, win_index=PINFU_WIN, ura_dora_indicators=["4p"])
    assert not result["ok"]


@pytest.mark.parametrize("bad", ["8z", "0z", "xm", "10m"])
def test_unknown_tiles_are_rejected(bad):
    result = run(tiles=[bad], win_index=0)
    assert result["error"].startswith("Unknown tile")
