package com.tilemaster.scoring

import com.tilemaster.model.Tile

enum class YakuCategory(val title: String, val blurb: String) {
    SITUATIONAL("Winning situations", "Earned by how you won, not by the tiles you hold."),
    SHAPE("Hand shapes", "Earned by the pattern your tiles make."),
    LIMIT("Limit hands", "Rare hands worth a fixed score."),
    YAKUMAN("Yakuman", "The biggest hands in the game."),
}

/** How a yaku is valued: a han count, or a fixed limit. */
sealed interface YakuValue {
    /** [closed] han with a closed hand, [open] han once you've called a tile, or null if it needs a closed hand. */
    data class Han(val closed: Int, val open: Int?) : YakuValue
    data object Mangan : YakuValue
    data object Yakuman : YakuValue
}

/**
 * One row of the yaku list. [guideKey] looks up the explanation in [YakuGuide]; set [description]
 * for entries that cover several guide entries. [example] is a winning hand in standard notation,
 * where spaces split the tiles into groups (see [parseTiles]).
 */
data class YakuEntry(
    val name: String,
    val guideKey: String,
    val category: YakuCategory,
    val value: YakuValue,
    val example: String? = null,
    val meaning: String? = null,
    val description: String? = null,
) {
    val info: YakuInfo? get() = YakuGuide.lookup(guideKey)
    val shownMeaning: String get() = meaning ?: info?.meaning.orEmpty()
    val shownDescription: String get() = description ?: info?.description.orEmpty()
    val exampleGroups: List<List<Tile>> get() = example?.let(::parseTiles).orEmpty()
}

/** Expands notation like `"123m 55z"` into tile groups: `[[1m, 2m, 3m], [5z, 5z]]`. */
fun parseTiles(notation: String): List<List<Tile>> =
    notation.trim().split(' ').filter { it.isNotEmpty() }.map { token ->
        val suit = token.last()
        token.dropLast(1).map { Tile.fromId("$it$suit") ?: error("Bad tile $it$suit in \"$notation\"") }
    }

object YakuCatalog {

    private fun han(closed: Int, open: Int?) = YakuValue.Han(closed, open)

    private val S = YakuCategory.SITUATIONAL
    private val H = YakuCategory.SHAPE
    private val L = YakuCategory.LIMIT
    private val Y = YakuCategory.YAKUMAN

    val entries: List<YakuEntry> = listOf(
        // Winning situations
        YakuEntry("Riichi", "Riichi", S, han(1, null)),
        YakuEntry("Double Riichi", "Double Riichi", S, han(2, null)),
        YakuEntry("Ippatsu", "Ippatsu", S, han(1, null)),
        YakuEntry("Menzen Tsumo", "Menzen Tsumo", S, han(1, null)),
        YakuEntry("Rinshan Kaihou", "Rinshan Kaihou", S, han(1, 1)),
        YakuEntry("Chankan", "Chankan", S, han(1, 1)),
        YakuEntry("Haitei Raoyue", "Haitei Raoyue", S, han(1, 1)),
        YakuEntry("Houtei Raoyui", "Houtei Raoyui", S, han(1, 1)),

        // Hand shapes, by value
        YakuEntry("Tanyao", "Tanyao", H, han(1, 1), "234m 567m 345p 678s 44p"),
        YakuEntry("Pinfu", "Pinfu", H, han(1, null), "123m 456m 789p 234s 66s"),
        YakuEntry("Iipeiko", "Iipeiko", H, han(1, null), "223344m 567p 789s 44p"),
        YakuEntry(
            "Yakuhai", "Yakuhai (haku)", H, han(1, 1), "555z 234m 567p 789s 99p",
            meaning = "Value tile triplet",
            description = "A triplet or quad of a dragon, your seat wind or the round wind. " +
                "Each one is worth 1 han, and a wind that is both your seat and the round counts twice.",
        ),
        YakuEntry("Chiitoitsu", "Chiitoitsu", H, han(2, null), "11m 55m 33p 99p 22s 77s 44z"),
        YakuEntry("Sanshoku Doujun", "Sanshoku Doujun", H, han(2, 1), "456m 456p 456s 789m 22p"),
        YakuEntry("Ittsu", "Ittsu", H, han(2, 1), "123456789m 345p 66s"),
        YakuEntry("Chantai", "Chantai", H, han(2, 1), "123m 789p 999s 111z 55z"),
        YakuEntry("Toitoi", "Toitoi", H, han(2, 2), "222m 888p 444s 777s 11z"),
        YakuEntry("San Ankou", "San Ankou", H, han(2, 2), "555m 888p 222s 345s 99p"),
        YakuEntry("Sanshoku Doukou", "Sanshoku Doukou", H, han(2, 2), "777m 777p 777s 234m 99p"),
        YakuEntry("San Kantsu", "San Kantsu", H, han(2, 2), "1111m 5555p 9999s"),
        YakuEntry("Honroutou", "Honroutou", H, han(2, 2), "111m 999p 111s 555z 99m"),
        YakuEntry("Shou Sangen", "Shou Sangen", H, han(2, 2), "555z 666z 77z 234m 456p"),
        YakuEntry("Honitsu", "Honitsu", H, han(3, 2), "123m 456m 789m 111z 55z"),
        YakuEntry("Junchan", "Junchan", H, han(3, 2), "123m 789m 123p 999s 11s"),
        YakuEntry("Ryanpeikou", "Ryanpeikou", H, han(3, null), "223344m 556677p 99s"),
        YakuEntry("Chinitsu", "Chinitsu", H, han(6, 5), "123m 456m 789m 234m 55m"),

        // Limit hands
        YakuEntry("Nagashi Mangan", "Nagashi Mangan", L, YakuValue.Mangan),

        // Yakuman
        YakuEntry("Tenhou", "Tenhou", Y, YakuValue.Yakuman),
        YakuEntry("Chiihou", "Chiihou", Y, YakuValue.Yakuman),
        YakuEntry("Kokushi Musou", "Kokushi Musou", Y, YakuValue.Yakuman, "19m 19p 19s 1234567z 1z"),
        YakuEntry("Suu Ankou", "Suu Ankou", Y, YakuValue.Yakuman, "111m 444p 777s 222z 55m"),
        YakuEntry("Daisangen", "Daisangen", Y, YakuValue.Yakuman, "555z 666z 777z 234m 99p"),
        YakuEntry("Shousuushii", "Shousuushii", Y, YakuValue.Yakuman, "111z 222z 333z 44z 567m"),
        YakuEntry("Dai Suushii", "Dai Suushii", Y, YakuValue.Yakuman, "111z 222z 333z 444z 55m"),
        YakuEntry("Tsuu Iisou", "Tsuu Iisou", Y, YakuValue.Yakuman, "111z 222z 555z 666z 77z"),
        YakuEntry("Chinroutou", "Chinroutou", Y, YakuValue.Yakuman, "111m 999m 111p 999p 11s"),
        YakuEntry("Ryuuiisou", "Ryuuiisou", Y, YakuValue.Yakuman, "234s 234s 666s 888s 66z"),
        YakuEntry("Chuuren Poutou", "Chuuren Poutou", Y, YakuValue.Yakuman, "1112345678999m 2m"),
        YakuEntry("Suu Kantsu", "Suu Kantsu", Y, YakuValue.Yakuman),
    )
}
