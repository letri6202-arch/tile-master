package com.tilemaster.scoring

/** Whether a yaku needs a closed hand, and what calling does to it. */
enum class YakuRule(val label: String?) {
    ANY(null),
    CLOSED_ONLY("Closed hand only"),
    LESS_WHEN_OPEN("1 han less when open"),
}

/** A plain-English explanation of one yaku, for players who don't know the Japanese names. */
data class YakuInfo(val meaning: String, val description: String, val rule: YakuRule = YakuRule.ANY)

/**
 * Explanations keyed by the yaku names the `mahjong` library returns (plus "Ura Dora", which
 * scorer.py splits out of Dora).
 */
object YakuGuide {

    fun lookup(name: String): YakuInfo? = ENTRIES[name]

    private val closed = YakuRule.CLOSED_ONLY
    private val lessOpen = YakuRule.LESS_WHEN_OPEN

    private val ENTRIES: Map<String, YakuInfo> = mapOf(
        // Winning situations
        "Riichi" to YakuInfo("Ready hand", "You declared that your closed hand was one tile from winning.", closed),
        "Double Riichi" to YakuInfo(
            "Riichi on the first turn",
            "You declared riichi on your very first discard, before anyone called a tile.",
            closed,
        ),
        "Open Riichi" to YakuInfo("Riichi with the hand shown", "You declared riichi and revealed your waiting hand.", closed),
        "Double Open Riichi" to YakuInfo(
            "First-turn open riichi",
            "You declared riichi on your first discard and revealed your waiting hand.",
            closed,
        ),
        "Ippatsu" to YakuInfo(
            "One shot",
            "You won within one go-around of declaring riichi, with no calls in between.",
            closed,
        ),
        "Menzen Tsumo" to YakuInfo(
            "Self-draw with a closed hand",
            "You drew the winning tile yourself without having called any tiles.",
            closed,
        ),
        "Rinshan Kaihou" to YakuInfo("Win after a kan", "You won on the replacement tile drawn after declaring a kan."),
        "Chankan" to YakuInfo("Robbing a kan", "You won on a tile another player added to their pon to make a kan."),
        "Haitei Raoyue" to YakuInfo("Last tile from the wall", "You won by tsumo on the last tile that could be drawn."),
        "Houtei Raoyui" to YakuInfo("Last discard", "You won by ron on the final discard of the hand."),

        // Hand shapes
        "Pinfu" to YakuInfo(
            "All sequences",
            "Four sequences and a pair that isn't a dragon or a valuable wind, won on a two-sided wait.",
            closed,
        ),
        "Tanyao" to YakuInfo("All simples", "Every tile is a number from 2 to 8. No 1s, 9s, winds or dragons."),
        "Iipeiko" to YakuInfo("Two identical sequences", "Two copies of the same sequence, like 3-4-5 circles twice.", closed),
        "Ryanpeikou" to YakuInfo(
            "Two pairs of identical sequences",
            "Two different sequences, each appearing twice. Replaces iipeiko.",
            closed,
        ),
        "Chiitoitsu" to YakuInfo("Seven pairs", "Seven different pairs instead of the usual four sets and a pair.", closed),
        "Sanshoku Doujun" to YakuInfo(
            "Same sequence in all three suits",
            "The same run in characters, circles and bamboo, like 4-5-6 in each.",
            lessOpen,
        ),
        "Sanshoku Doukou" to YakuInfo(
            "Same triplet in all three suits",
            "A triplet of the same number in characters, circles and bamboo.",
        ),
        "Ittsu" to YakuInfo("Straight", "1-2-3, 4-5-6 and 7-8-9 all in one suit.", lessOpen),
        "Chantai" to YakuInfo(
            "Terminals or honors in every set",
            "Every set and the pair include a 1, a 9, a wind or a dragon.",
            lessOpen,
        ),
        "Junchan" to YakuInfo(
            "Terminals in every set",
            "Every set and the pair include a 1 or a 9, with no winds or dragons.",
            lessOpen,
        ),
        "Honroutou" to YakuInfo("All terminals and honors", "Every tile is a 1, a 9, a wind or a dragon."),
        "Toitoi" to YakuInfo("All triplets", "Four triplets or quads and a pair. No sequences."),
        "San Ankou" to YakuInfo(
            "Three concealed triplets",
            "Three triplets you made yourself rather than by calling. A triplet finished by ron doesn't count.",
        ),
        "San Kantsu" to YakuInfo("Three quads", "You declared three kans."),
        "Shou Sangen" to YakuInfo("Little three dragons", "Triplets of two dragons and a pair of the third."),
        "Honitsu" to YakuInfo("Half flush", "Tiles from only one suit, plus winds and dragons.", lessOpen),
        "Chinitsu" to YakuInfo("Full flush", "Every tile is from one suit. No winds or dragons.", lessOpen),

        // Value tiles
        "Yakuhai (haku)" to YakuInfo("White dragon triplet", "A triplet or quad of white dragons."),
        "Yakuhai (hatsu)" to YakuInfo("Green dragon triplet", "A triplet or quad of green dragons."),
        "Yakuhai (chun)" to YakuInfo("Red dragon triplet", "A triplet or quad of red dragons."),
        "Yakuhai (wind of place)" to YakuInfo("Seat wind triplet", "A triplet or quad of your own seat wind."),
        "Yakuhai (wind of round)" to YakuInfo("Round wind triplet", "A triplet or quad of the round wind."),
        "Yakuhai (east)" to YakuInfo("East wind triplet", "A triplet or quad of East when it's your seat or the round wind."),
        "Yakuhai (south)" to YakuInfo("South wind triplet", "A triplet or quad of South when it's your seat or the round wind."),
        "Yakuhai (west)" to YakuInfo("West wind triplet", "A triplet or quad of West when it's your seat or the round wind."),
        "Yakuhai (north)" to YakuInfo("North wind triplet", "A triplet or quad of North when it's your seat or the round wind."),

        // Bonus han (these don't count as yaku on their own)
        "Dora" to YakuInfo("Bonus tiles", "One han for each tile matching a dora indicator's next tile. Doesn't count as a yaku."),
        "Ura Dora" to YakuInfo(
            "Hidden bonus tiles",
            "Dora from the tiles under the indicators, revealed only for riichi winners. Doesn't count as a yaku.",
        ),
        "Aka Dora" to YakuInfo("Red fives", "One han for each red five in your hand. Doesn't count as a yaku."),

        // Limit hands
        "Nagashi Mangan" to YakuInfo(
            "Mangan at a draw",
            "At an exhaustive draw, all your discards were 1s, 9s and honors, and none were called.",
        ),
        "Renhou" to YakuInfo(
            "Hand of man",
            "You won by ron before your first draw, with no calls made. Scored as a mangan.",
            closed,
        ),
        "Renhou (yakuman)" to YakuInfo("Hand of man", "You won by ron before your first draw, with no calls made.", closed),
        "Tenhou" to YakuInfo("Blessing of heaven", "As dealer, your starting hand was already complete.", closed),
        "Chiihou" to YakuInfo(
            "Blessing of earth",
            "As a non-dealer, you won on your first draw before anyone called.",
            closed,
        ),
        "Kokushi Musou" to YakuInfo(
            "Thirteen orphans",
            "One of each 1, 9, wind and dragon, plus one extra of any of them.",
            closed,
        ),
        "Kokushi Musou Juusanmen Matchi" to YakuInfo(
            "Thirteen orphans, 13-sided wait",
            "You held one of each 1, 9, wind and dragon, so any of them would win.",
            closed,
        ),
        "Suu Ankou" to YakuInfo("Four concealed triplets", "Four triplets you made yourself, with none called.", closed),
        "Suu Ankou Tanki" to YakuInfo(
            "Four concealed triplets, pair wait",
            "Four concealed triplets, won on the tile that completed the pair.",
            closed,
        ),
        "Daisangen" to YakuInfo("Big three dragons", "Triplets or quads of all three dragons."),
        "Shousuushii" to YakuInfo("Little four winds", "Triplets of three winds and a pair of the fourth."),
        "Dai Suushii" to YakuInfo("Big four winds", "Triplets or quads of all four winds."),
        "Tsuu Iisou" to YakuInfo("All honors", "Every tile is a wind or a dragon."),
        "Chinroutou" to YakuInfo("All terminals", "Every tile is a 1 or a 9."),
        "Ryuuiisou" to YakuInfo("All green", "Only 2, 3, 4, 6 and 8 bamboo and the green dragon."),
        "Chuuren Poutou" to YakuInfo(
            "Nine gates",
            "1-1-1-2-3-4-5-6-7-8-9-9-9 in one suit, plus one more tile of that suit.",
            closed,
        ),
        "Daburu Chuuren Poutou" to YakuInfo(
            "Pure nine gates",
            "You held 1-1-1-2-3-4-5-6-7-8-9-9-9 in one suit, so any tile of that suit would win.",
            closed,
        ),
        "Suu Kantsu" to YakuInfo("Four quads", "You declared four kans."),
        "Daisharin" to YakuInfo("Big wheels", "Pairs of 2 through 8 in circles.", closed),
        "Daichisei" to YakuInfo("Big seven stars", "Seven pairs made entirely of honor tiles.", closed),
        "Paarenchan" to YakuInfo("Eight consecutive wins", "The dealer won eight hands in a row."),
        "Sashikomi" to YakuInfo("Dealing in on purpose", "A house rule for open riichi: dealing into it is a yakuman."),
    )
}
