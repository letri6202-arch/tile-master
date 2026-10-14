package com.tilemaster.scoring

import com.tilemaster.model.Tile
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreJsonTest {

    @Test
    fun requestAddsFaceDownKanTiles() {
        val tiles = listOf(Tile.CHUN, Tile.CHUN, Tile.M2, Tile.M3, Tile.M4)
        val context = HandContext(
            melds = listOf(Meld(MeldType.CLOSED_KAN, listOf(0, 1))),
            winIndex = 4,
            isTsumo = true,
            seatWind = Wind.WEST,
            roundWind = Wind.SOUTH,
            riichi = Riichi.DOUBLE,
            doraIndicators = listOf(Tile.P0),
            honba = 2,
            riichiSticks = 1,
        )

        val json = JSONObject(ScoreRequest.toJson(tiles, context))

        assertEquals(listOf("7z", "7z", "2m", "3m", "4m", "7z", "7z"), json.getJSONArray("tiles").toList())
        val meld = json.getJSONArray("melds").getJSONObject(0)
        assertEquals("kan", meld.getString("type"))
        assertEquals(listOf(0, 1, 5, 6), meld.getJSONArray("indices").toList())
        assertEquals(false, meld.getBoolean("opened"))
        assertEquals(4, json.getInt("win_index"))
        assertEquals("double", json.getString("riichi"))
        assertEquals("W", json.getString("seat_wind"))
        assertEquals("S", json.getString("round_wind"))
        assertEquals(listOf("0p"), json.getJSONArray("dora_indicators").toList())
        assertEquals(2, json.getInt("honba"))
        assertEquals(1, json.getInt("riichi_sticks"))
    }

    @Test
    fun parsesNonDealerTsumo() {
        val response = """
            {"ok": true, "han": 3, "fu": 60, "is_open_hand": false, "is_dealer": false,
             "is_yakuman": false, "limit": "",
             "yaku": [{"name": "Menzen Tsumo", "han": 1, "is_yakuman": false}],
             "fu_details": [{"fu": 32, "reason": "closed_terminal_kan"}, {"fu": 20, "reason": "base"}],
             "payment": {"main": 3900, "main_bonus": 100, "additional": 2000, "additional_bonus": 100,
                         "riichi_sticks": 1000, "total": 9200}}
        """.trimIndent()

        val result = (ScoreOutcome.fromJson(response, isTsumo = true) as ScoreOutcome.Scored).result

        assertEquals("3 han 60 fu", result.headline)
        assertEquals("2,100 / 4,000", result.callout)
        assertEquals(
            listOf(
                PaymentLine("From the dealer", 4000),
                PaymentLine("From each non-dealer", 2100),
                PaymentLine("Riichi sticks on the table", 1000),
            ),
            result.payments,
        )
        assertEquals(300, result.honbaBonus)
        assertEquals("Closed terminal/honor kan", result.fuDetails[0].label)
    }

    @Test
    fun namesLimitHands() {
        fun headline(limit: String, yakuman: Boolean = false) = ScoreResult(
            han = 13, fu = 30, isTsumo = false, isOpenHand = false, isDealer = true, isYakuman = yakuman,
            limit = limit, yaku = emptyList(), fuDetails = emptyList(), main = 0, mainBonus = 0,
            additional = 0, additionalBonus = 0, riichiSticks = 0, total = 0,
        ).headline

        assertEquals("Mangan", headline("mangan"))
        assertEquals("Yakuman", headline("yakuman", yakuman = true))
        assertEquals("Double yakuman", headline("2x yakuman", yakuman = true))
        assertEquals("Kazoe yakuman", headline("kazoe yakuman"))
    }

    @Test
    fun parsesFailure() {
        val outcome = ScoreOutcome.fromJson("""{"ok": false, "error": "The hand has no yaku."}""", isTsumo = false)
        assertEquals(ScoreOutcome.Failed("The hand has no yaku."), outcome)
    }

    @Test
    fun dealerTsumoCalloutSaysAll() {
        val response = """
            {"ok": true, "han": 13, "fu": 0, "is_open_hand": false, "is_dealer": true, "is_yakuman": true,
             "limit": "yakuman", "yaku": [{"name": "Kokushi Musou", "han": 13, "is_yakuman": true}],
             "fu_details": [], "payment": {"main": 16000, "main_bonus": 0, "additional": 16000,
             "additional_bonus": 0, "riichi_sticks": 0, "total": 48000}}
        """.trimIndent()
        val result = (ScoreOutcome.fromJson(response, isTsumo = true) as ScoreOutcome.Scored).result
        assertEquals("16,000 all", result.callout)
        assertEquals("Yakuman", result.yaku.single().hanLabel)
        assertTrue(result.detail == null)
    }

    private fun org.json.JSONArray.toList(): List<Any> = List(length()) { get(it) }
}
