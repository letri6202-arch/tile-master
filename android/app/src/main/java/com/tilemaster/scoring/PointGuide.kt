package com.tilemaster.scoring

/** Content of the point calculation guide, in reading order. Standard riichi rules. */
object PointGuide {

    val intro = "Every winning hand is scored in three moves: count han, count fu, then turn them into points."

    val sections: List<GuideSection> = listOf(
        GuideSection(
            "Step 1: Count han",
            "Han measure how many scoring features your hand has.",
            listOf(
                GuideCard(
                    "Add up your han",
                    "Each yaku is worth a set number of han, and many are worth less once you've called a tile " +
                        "(see the Yaku List). Then add one han for every dora, ura dora and red five in the hand.",
                    notes = listOf(
                        "You need at least one yaku to win. Dora alone don't count as a yaku.",
                        "Yakuman hands skip the rest of the maths and go straight to a limit score.",
                    ),
                ),
            ),
        ),
        GuideSection(
            "Step 2: Count fu",
            "Fu reward harder-to-build shapes. Start at 20 and add up the bonuses below.",
            listOf(
                GuideCard(
                    "How you won",
                    table = GuideTable(
                        listOf("Source", "Fu"),
                        listOf(
                            listOf("Starting value", "20"),
                            listOf("Won by ron with a closed hand", "+10"),
                            listOf("Won by tsumo", "+2"),
                        ),
                    ),
                ),
                GuideCard(
                    "Triplets and quads",
                    table = GuideTable(
                        listOf("Set", "Open", "Closed"),
                        listOf(
                            listOf("Triplet of 2 to 8", "2", "4"),
                            listOf("Triplet of 1, 9, wind or dragon", "4", "8"),
                            listOf("Quad of 2 to 8", "8", "16"),
                            listOf("Quad of 1, 9, wind or dragon", "16", "32"),
                        ),
                    ),
                    notes = listOf("A triplet you completed by ron counts as open. Sequences are worth nothing."),
                ),
                GuideCard(
                    "Pair and wait",
                    table = GuideTable(
                        listOf("Source", "Fu"),
                        listOf(
                            listOf("Pair of dragons, your seat wind or the round wind", "+2"),
                            listOf("Waiting on one tile: middle, edge or pair", "+2"),
                            listOf("Two-sided wait", "0"),
                        ),
                    ),
                    notes = listOf("Most rules give a pair of a wind that is both your seat and the round wind +4."),
                ),
                GuideCard(
                    "Special cases",
                    table = GuideTable(
                        listOf("Hand", "Fu"),
                        listOf(
                            listOf("Seven pairs", "25"),
                            listOf("Pinfu won by tsumo", "20"),
                            listOf("Open hand that would score 20 on ron", "30"),
                        ),
                    ),
                    notes = listOf("Everywhere else, round the total up to the next 10. For example 22 becomes 30."),
                ),
            ),
        ),
        GuideSection(
            "Step 3: Basic points",
            "Han and fu combine into one number that every payment is built from.",
            listOf(
                GuideCard(
                    "The formula",
                    "Each extra han doubles the score, and fu scale it up.",
                    formula = "fu × 2^(han + 2)",
                    notes = listOf(
                        "Basic points are capped at 2,000. That cap is a mangan, reached by 5 han, or by 4 han " +
                            "at 40 fu or more, or 3 han at 70 fu or more.",
                        "Some rules also round 4 han 30 fu and 3 han 60 fu up to mangan.",
                    ),
                ),
                GuideCard(
                    "Limit hands",
                    "Past a certain size, han alone decide the score and fu stop mattering.",
                    table = GuideTable(
                        listOf("Name", "Han", "Non-dealer", "Dealer"),
                        listOf(
                            listOf("Mangan", "5", "8,000", "12,000"),
                            listOf("Haneman", "6 to 7", "12,000", "18,000"),
                            listOf("Baiman", "8 to 10", "16,000", "24,000"),
                            listOf("Sanbaiman", "11 to 12", "24,000", "36,000"),
                            listOf("Yakuman", "13+ or a yakuman hand", "32,000", "48,000"),
                        ),
                    ),
                    notes = listOf("Scores shown are for a win by ron. Each extra yakuman adds another full yakuman."),
                ),
            ),
        ),
        GuideSection(
            "Step 4: Paying out",
            "Multiply the basic points by who won and how, then round each payment up to the next 100.",
            listOf(
                GuideCard(
                    "Who pays what",
                    table = GuideTable(
                        listOf("Win", "Payment"),
                        listOf(
                            listOf("Non-dealer, ron", "Discarder pays basic × 4"),
                            listOf("Dealer, ron", "Discarder pays basic × 6"),
                            listOf("Non-dealer, tsumo", "Dealer pays × 2, others × 1 each"),
                            listOf("Dealer, tsumo", "Everyone pays × 2"),
                        ),
                    ),
                ),
                GuideCard(
                    "Extras on top",
                    table = GuideTable(
                        null,
                        listOf(
                            listOf("Each honba (repeat counter)", "+300"),
                            listOf("Each riichi stick on the table", "+1,000"),
                        ),
                    ),
                    notes = listOf(
                        "On a tsumo the honba bonus is split, 100 from each payer.",
                        "The winner takes every riichi stick on the table, including their own.",
                    ),
                ),
            ),
        ),
        GuideSection(
            "Worked examples",
            "The same steps on two real hands.",
            listOf(
                GuideCard(
                    "Non-dealer wins by ron",
                    table = GuideTable(
                        null,
                        listOf(
                            listOf("Han", "Riichi + ippatsu + 1 dora = 3"),
                            listOf("Fu", "20 + 10 closed ron + 8 closed 9-pin triplet = 38, so 40"),
                            listOf("Basic points", "40 × 2^5 = 1,280"),
                            listOf("Payment", "1,280 × 4 = 5,120, so 5,200 from the discarder"),
                        ),
                    ),
                ),
                GuideCard(
                    "Dealer wins by tsumo",
                    table = GuideTable(
                        null,
                        listOf(
                            listOf("Han", "Riichi + menzen tsumo = 2"),
                            listOf("Fu", "20 + 2 tsumo + 4 closed simple triplet + 2 middle wait = 28, so 30"),
                            listOf("Basic points", "30 × 2^4 = 480"),
                            listOf("Payment", "480 × 2 = 960, so 1,000 from each player, 3,000 in total"),
                        ),
                    ),
                ),
            ),
        ),
    )
}
