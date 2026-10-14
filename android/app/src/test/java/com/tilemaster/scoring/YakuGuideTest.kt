package com.tilemaster.scoring

import org.junit.Assert.assertTrue
import org.junit.Test

class YakuGuideTest {

    /** Every name `mahjong` 1.4.0 can return (YakuConfig), plus "Ura Dora" from scorer.py. */
    private val libraryNames = listOf(
        "Menzen Tsumo", "Riichi", "Open Riichi", "Ippatsu", "Chankan", "Rinshan Kaihou", "Haitei Raoyue",
        "Houtei Raoyui", "Double Riichi", "Double Open Riichi", "Nagashi Mangan", "Renhou", "Pinfu", "Tanyao",
        "Iipeiko", "Yakuhai (haku)", "Yakuhai (hatsu)", "Yakuhai (chun)", "Yakuhai (east)", "Yakuhai (south)",
        "Yakuhai (west)", "Yakuhai (north)", "Yakuhai (wind of place)", "Yakuhai (wind of round)",
        "Sanshoku Doujun", "Ittsu", "Chantai", "Honroutou", "Toitoi", "San Ankou", "San Kantsu",
        "Sanshoku Doukou", "Chiitoitsu", "Shou Sangen", "Honitsu", "Junchan", "Ryanpeikou", "Chinitsu",
        "Kokushi Musou", "Chuuren Poutou", "Suu Ankou", "Daisangen", "Shousuushii", "Ryuuiisou", "Suu Kantsu",
        "Tsuu Iisou", "Chinroutou", "Daisharin", "Daichisei", "Dai Suushii", "Kokushi Musou Juusanmen Matchi",
        "Suu Ankou Tanki", "Daburu Chuuren Poutou", "Tenhou", "Chiihou", "Renhou (yakuman)", "Sashikomi",
        "Paarenchan", "Dora", "Aka Dora", "Ura Dora",
    )

    @Test
    fun everyYakuTheLibraryReturnsIsExplained() {
        val missing = libraryNames.filter { YakuGuide.lookup(it) == null }
        assertTrue("No explanation for: $missing", missing.isEmpty())
    }
}
