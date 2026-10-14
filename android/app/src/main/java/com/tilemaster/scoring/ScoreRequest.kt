package com.tilemaster.scoring

import com.tilemaster.model.Tile
import org.json.JSONArray
import org.json.JSONObject

/** Builds the request JSON for `scorer.score` (see src/main/python/scorer.py). */
object ScoreRequest {

    fun toJson(tiles: List<Tile>, context: HandContext): String {
        val winIndex = requireNotNull(context.winIndex) { "No winning tile" }
        val seatWind = requireNotNull(context.seatWind) { "No seat wind" }
        val isTsumo = requireNotNull(context.isTsumo) { "No win type" }

        // Face-down kan tiles weren't scanned; append them so every kan has 4 tiles.
        val allTiles = tiles.toMutableList()
        val melds = JSONArray()
        context.melds.forEach { meld ->
            val indices = meld.indices.toMutableList()
            repeat(meld.hiddenCopies) {
                indices += allTiles.size
                allTiles += HandContext.plainTile(tiles[meld.indices.first()])
            }
            melds.put(
                JSONObject()
                    .put("type", if (meld.type.isKan) "kan" else meld.type.name.lowercase())
                    .put("indices", JSONArray(indices))
                    .put("opened", meld.type != MeldType.CLOSED_KAN),
            )
        }

        return JSONObject()
            .put("tiles", JSONArray(allTiles.map { it.id }))
            .put("win_index", winIndex)
            .put("melds", melds)
            .put("is_tsumo", isTsumo)
            .put("riichi", context.riichi.name.lowercase())
            .put("is_ippatsu", context.isIppatsu)
            .put("is_rinshan", context.isRinshan)
            .put("is_chankan", context.isChankan)
            .put("is_haitei", context.isHaitei)
            .put("is_houtei", context.isHoutei)
            .put("is_tenhou", context.isTenhou)
            .put("is_chiihou", context.isChiihou)
            .put("is_renhou", context.isRenhou)
            .put("seat_wind", seatWind.code)
            .put("round_wind", context.roundWind.code)
            .put("dora_indicators", JSONArray(context.doraIndicators.map { it.id }))
            .put("ura_dora_indicators", JSONArray(context.uraDoraIndicators.map { it.id }))
            .put("honba", context.honba)
            .put("riichi_sticks", context.riichiSticks)
            .toString()
    }
}
