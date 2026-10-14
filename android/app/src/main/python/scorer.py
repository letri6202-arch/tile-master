"""
Scores one winning Riichi hand with the `mahjong` library.

Ported from the Mahjong Vision backend (score_calculator.py + test_mahjong.py). The app calls
`score(request_json)` through Chaquopy and gets a JSON string back, so this module has no
Android dependencies and can be tested on a desktop with pytest.

Tiles use the app's notation: `1m`..`9m`, `1p`..`9p`, `1s`..`9s`, `1z`..`7z`
(East, South, West, North, Haku, Hatsu, Chun) and `0m`/`0p`/`0s` for red fives.

Request:
    {
      "tiles": ["1m", ...],          # every tile in the hand, called melds included
      "win_index": 13,               # index into `tiles` of the winning tile
      "melds": [{"type": "pon" | "chi" | "kan", "indices": [0, 1, 2], "opened": true}],
      "is_tsumo": false,
      "riichi": "none" | "riichi" | "double",
      "is_ippatsu": false, "is_rinshan": false, "is_chankan": false,
      "is_haitei": false, "is_houtei": false,
      "is_tenhou": false, "is_chiihou": false, "is_renhou": false,
      "seat_wind": "E", "round_wind": "E",
      "dora_indicators": ["3p"], "ura_dora_indicators": [],
      "honba": 0, "riichi_sticks": 0
    }

Response (success):
    {"ok": true, "han", "fu", "is_open_hand", "is_dealer", "is_yakuman", "limit",
     "yaku": [{"name", "han", "is_yakuman"}], "fu_details": [{"fu", "reason"}],
     "payment": {"main", "main_bonus", "additional", "additional_bonus", "riichi_sticks", "total"}}

Response (failure):
    {"ok": false, "error": "<message for the player>"}
"""

import json

from mahjong.constants import EAST, NORTH, SOUTH, WEST
from mahjong.hand_calculating.hand import HandCalculator
from mahjong.hand_calculating.hand_config import HandConfig, OptionalRules
from mahjong.meld import Meld
from mahjong.utils import plus_dora

WINDS = {"E": EAST, "S": SOUTH, "W": WEST, "N": NORTH}

SUIT_OFFSET = {"m": 0, "p": 9, "s": 18, "z": 27}

MELD_TYPES = {"chi": Meld.CHI, "pon": Meld.PON, "kan": Meld.KAN}

# Same table rules as Mahjong Vision.
RULES = OptionalRules(
    has_open_tanyao=True,
    has_aka_dora=True,
    renhou_as_yakuman=False,
    kazoe_limit=HandConfig.KAZOE_LIMITED,
    kiriage=False,
    has_double_yakuman=True,
)

ERROR_MESSAGES = {
    HandCalculator.ERR_NO_WINNING_TILE: "The winning tile is not in the hand.",
    HandCalculator.ERR_OPEN_HAND_RIICHI: "You can't riichi with an open hand.",
    HandCalculator.ERR_OPEN_HAND_DABURI: "You can't double riichi with an open hand.",
    HandCalculator.ERR_IPPATSU_WITHOUT_RIICHI: "Ippatsu needs riichi.",
    HandCalculator.ERR_HAND_NOT_WINNING: "This isn't a complete winning hand.",
    HandCalculator.ERR_HAND_NOT_CORRECT: "The hand has the wrong number of tiles.",
    HandCalculator.ERR_NO_YAKU: "The hand has no yaku, so it can't win.",
    HandCalculator.ERR_CHANKAN_WITH_TSUMO: "Chankan is a ron win, not tsumo.",
    HandCalculator.ERR_RINSHAN_WITHOUT_TSUMO: "Rinshan is a tsumo win.",
    HandCalculator.ERR_HAITEI_WITHOUT_TSUMO: "Haitei is a tsumo win.",
    HandCalculator.ERR_HOUTEI_WITH_TSUMO: "Houtei is a ron win, not tsumo.",
    HandCalculator.ERR_HAITEI_WITH_RINSHAN: "Haitei and rinshan can't both apply.",
    HandCalculator.ERR_HOUTEI_WITH_CHANKAN: "Houtei and chankan can't both apply.",
    HandCalculator.ERR_TENHOU_NOT_AS_DEALER: "Tenhou is only for the dealer.",
    HandCalculator.ERR_TENHOU_WITHOUT_TSUMO: "Tenhou is a tsumo win.",
    HandCalculator.ERR_TENHOU_WITH_MELD: "Tenhou can't have called tiles.",
    HandCalculator.ERR_CHIIHOU_AS_DEALER: "Chiihou is only for non-dealers.",
    HandCalculator.ERR_CHIIHOU_WITHOUT_TSUMO: "Chiihou is a tsumo win.",
    HandCalculator.ERR_CHIIHOU_WITH_MELD: "Chiihou can't have called tiles.",
    HandCalculator.ERR_RENHOU_AS_DEALER: "Renhou is only for non-dealers.",
    HandCalculator.ERR_RENHOU_WITH_TSUMO: "Renhou is a ron win, not tsumo.",
    HandCalculator.ERR_RENHOU_WITH_MELD: "Renhou can't have called tiles.",
}


class InvalidHand(ValueError):
    """Input the player can fix; the message is shown to them."""


def score(request_json):
    """Entry point for the app. Takes and returns JSON strings."""
    try:
        return json.dumps(_score(json.loads(request_json)))
    except InvalidHand as e:
        return json.dumps({"ok": False, "error": str(e)})


def _tile_34(tile_id):
    """`5m` -> 4, `0p` -> 13, `7z` -> 33. Returns (index_34, is_red_five)."""
    if len(tile_id) != 2 or tile_id[1] not in SUIT_OFFSET or not tile_id[0].isdigit():
        raise InvalidHand("Unknown tile: %s" % tile_id)
    rank, suit = int(tile_id[0]), tile_id[1]
    is_red = rank == 0
    if is_red:
        if suit == "z":
            raise InvalidHand("Unknown tile: %s" % tile_id)
        rank = 5
    if not 1 <= rank <= (7 if suit == "z" else 9):
        raise InvalidHand("Unknown tile: %s" % tile_id)
    return SUIT_OFFSET[suit] + rank - 1, is_red


def _to_136(tile_ids):
    """
    Gives each tile its own 136-format id. With aka dora on, copy 0 of each five is the red one,
    so red fives take copy 0 and plain fives take copies 1-3.
    """
    used = {}
    result = []
    for tile_id in tile_ids:
        index_34, is_red = _tile_34(tile_id)
        taken = used.setdefault(index_34, set())
        if is_red:
            copies = [0]
        elif index_34 in (4, 13, 22):
            copies = [1, 2, 3]
        else:
            copies = [0, 1, 2, 3]
        free = [c for c in copies if c not in taken]
        if not free:
            if is_red:
                raise InvalidHand("There is only one red five per suit.")
            raise InvalidHand("Too many copies of %s." % tile_id.replace("0", "5"))
        taken.add(free[0])
        result.append(index_34 * 4 + free[0])
    return result


def _indicators_136(tile_ids):
    # Dora only looks at the tile kind, so any copy will do.
    return [_tile_34(t)[0] * 4 for t in tile_ids]


def _wind(code, what):
    if code not in WINDS:
        raise InvalidHand("Pick a %s." % what)
    return WINDS[code]


def _score(req):
    tile_ids = req.get("tiles", [])
    tiles = _to_136(tile_ids)

    win_index = req.get("win_index")
    if not isinstance(win_index, int) or not 0 <= win_index < len(tiles):
        raise InvalidHand("Pick the winning tile.")

    melds = []
    meld_indices = set()
    for meld in req.get("melds", []):
        meld_type = MELD_TYPES.get(meld.get("type"))
        indices = meld.get("indices", [])
        if meld_type is None:
            raise InvalidHand("Unknown meld type: %s" % meld.get("type"))
        if len(indices) != (4 if meld_type == Meld.KAN else 3):
            raise InvalidHand("A %s has the wrong number of tiles." % meld.get("type"))
        if any(not 0 <= i < len(tiles) or i in meld_indices for i in indices):
            raise InvalidHand("A tile can only be in one meld.")
        meld_indices.update(indices)
        # Chi and pon are always called; only a kan can be closed.
        opened = meld.get("opened", True) if meld_type == Meld.KAN else True
        melds.append(Meld(meld_type=meld_type, tiles=[tiles[i] for i in indices], opened=opened))

    if win_index in meld_indices:
        raise InvalidHand("The winning tile can't be part of a called meld or kan.")

    riichi = req.get("riichi", "none")
    if riichi not in ("none", "riichi", "double"):
        raise InvalidHand("Unknown riichi option: %s" % riichi)

    ura_indicators = _indicators_136(req.get("ura_dora_indicators", []))
    if ura_indicators and riichi == "none":
        raise InvalidHand("Ura dora only counts with riichi.")
    dora_indicators = _indicators_136(req.get("dora_indicators", []))

    honba = int(req.get("honba", 0))
    riichi_sticks = int(req.get("riichi_sticks", 0))
    if honba < 0 or riichi_sticks < 0:
        raise InvalidHand("Honba and riichi sticks can't be negative.")

    config = HandConfig(
        is_tsumo=bool(req.get("is_tsumo", False)),
        is_riichi=riichi == "riichi",
        is_daburu_riichi=riichi == "double",
        is_ippatsu=bool(req.get("is_ippatsu", False)),
        is_rinshan=bool(req.get("is_rinshan", False)),
        is_chankan=bool(req.get("is_chankan", False)),
        is_haitei=bool(req.get("is_haitei", False)),
        is_houtei=bool(req.get("is_houtei", False)),
        is_tenhou=bool(req.get("is_tenhou", False)),
        is_chiihou=bool(req.get("is_chiihou", False)),
        is_renhou=bool(req.get("is_renhou", False)),
        player_wind=_wind(req.get("seat_wind"), "seat wind"),
        round_wind=_wind(req.get("round_wind"), "round wind"),
        tsumi_number=honba,
        kyoutaku_number=riichi_sticks,
        options=RULES,
    )

    # The library counts ura dora as regular dora, so pass both and split them back out below.
    result = HandCalculator().estimate_hand_value(
        tiles,
        tiles[win_index],
        melds=melds,
        dora_indicators=dora_indicators + ura_indicators,
        config=config,
    )
    if result.error:
        raise InvalidHand(ERROR_MESSAGES.get(result.error, "Can't score this hand (%s)." % result.error))

    is_yakuman = any(y.is_yakuman for y in result.yaku)
    yaku = []
    for y in result.yaku:
        han = y.han_open if result.is_open_hand else y.han_closed
        if y.name == "Dora" and ura_indicators:
            ura = sum(plus_dora(t, ura_indicators) for t in tiles)
            if han - ura:
                yaku.append({"name": "Dora", "han": han - ura, "is_yakuman": False})
            if ura:
                yaku.append({"name": "Ura Dora", "han": ura, "is_yakuman": False})
            continue
        yaku.append({"name": y.name, "han": han, "is_yakuman": bool(y.is_yakuman)})

    cost = result.cost
    return {
        "ok": True,
        "han": result.han,
        "fu": result.fu,
        "is_open_hand": result.is_open_hand,
        "is_dealer": config.is_dealer,
        "is_yakuman": is_yakuman,
        "limit": cost.get("yaku_level", ""),
        "yaku": yaku,
        "fu_details": result.fu_details or [],
        "payment": {
            "main": cost["main"],
            "main_bonus": cost["main_bonus"],
            "additional": cost["additional"],
            "additional_bonus": cost["additional_bonus"],
            "riichi_sticks": cost["kyoutaku_bonus"],
            "total": cost["total"],
        },
    }
