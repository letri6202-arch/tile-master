package com.tilemaster.model

enum class Suit(val code: Char) {
    MAN('m'),
    PIN('p'),
    SOU('s'),
    HONOR('z'),
}

/**
 * The 37 detectable tile classes. Declaration order is the model's class index order and must
 * match `ml/classes.py`. Ids use standard Riichi notation: `1m`..`9m`, `1p`..`9p`, `1s`..`9s`,
 * `1z`..`7z` (East, South, West, North, Haku, Hatsu, Chun) and `0m`/`0p`/`0s` for red fives.
 */
enum class Tile(val id: String, val displayName: String) {
    M1("1m", "1 Man"), M2("2m", "2 Man"), M3("3m", "3 Man"),
    M4("4m", "4 Man"), M5("5m", "5 Man"), M6("6m", "6 Man"),
    M7("7m", "7 Man"), M8("8m", "8 Man"), M9("9m", "9 Man"),

    P1("1p", "1 Pin"), P2("2p", "2 Pin"), P3("3p", "3 Pin"),
    P4("4p", "4 Pin"), P5("5p", "5 Pin"), P6("6p", "6 Pin"),
    P7("7p", "7 Pin"), P8("8p", "8 Pin"), P9("9p", "9 Pin"),

    S1("1s", "1 Sou"), S2("2s", "2 Sou"), S3("3s", "3 Sou"),
    S4("4s", "4 Sou"), S5("5s", "5 Sou"), S6("6s", "6 Sou"),
    S7("7s", "7 Sou"), S8("8s", "8 Sou"), S9("9s", "9 Sou"),

    EAST("1z", "East"), SOUTH("2z", "South"), WEST("3z", "West"), NORTH("4z", "North"),
    HAKU("5z", "White Dragon"), HATSU("6z", "Green Dragon"), CHUN("7z", "Red Dragon"),

    M0("0m", "Red 5 Man"), P0("0p", "Red 5 Pin"), S0("0s", "Red 5 Sou");

    val suit: Suit get() = Suit.entries.first { it.code == id[1] }

    /** 1-9 for suited tiles (red fives are 5), 1-7 for honors. */
    val rank: Int get() = id[0].digitToInt().let { if (it == 0) 5 else it }

    val isRedFive: Boolean get() = id[0] == '0'

    /** Large character printed on the tile face. */
    val faceTop: String
        get() = if (suit == Suit.HONOR) HONOR_KANJI[rank - 1] else rank.toString()

    /** Small suit character under the number; null for honors. */
    val faceBottom: String?
        get() = when (suit) {
            Suit.MAN -> "萬"
            Suit.PIN -> "筒"
            Suit.SOU -> "索"
            Suit.HONOR -> null
        }

    companion object {
        private val HONOR_KANJI = listOf("東", "南", "西", "北", "白", "發", "中")

        fun fromId(id: String): Tile? = entries.firstOrNull { it.id == id }

        fun fromClassIndex(index: Int): Tile? = entries.getOrNull(index)
    }
}
