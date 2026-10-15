package com.tilemaster.scoring

/** Content of the riichi rule book, in the order a new player needs it. Standard four-player rules. */
object RuleBook {

    val intro = "Riichi mahjong is a four-player tile game, a bit like rummy. You draw a tile, discard a tile, " +
        "and race to be the first to complete a winning hand."

    val sections: List<GuideSection> = listOf(
        GuideSection(
            "The big picture",
            "What the game is about, before any of the details.",
            listOf(
                GuideCard(
                    "The goal",
                    "Everyone builds a hand of 14 tiles. Whoever completes a valid hand first wins the round and " +
                        "collects points from the others. A full game is a series of these rounds, and the player " +
                        "with the most points at the end wins.",
                    bullets = listOf(
                        "Each turn you draw one tile and discard one, so your hand stays the same size.",
                        "You build toward a hand made of sets of tiles, called melds, plus a pair.",
                        "A finished hand must also contain a yaku, a named scoring pattern. This is the part " +
                            "that surprises most beginners.",
                    ),
                ),
                GuideCard(
                    "What a winning hand looks like",
                    "Almost every winning hand is four melds and one pair. Here 234 characters, 567 circles, " +
                        "789 bamboo and a triplet of red dragons are the four melds, and the two 9 circles are the pair.",
                    examples = listOf(TileExample("A complete hand: 4 melds + a pair = 14 tiles", "234m 567p 789s 777z 99p")),
                    notes = listOf("There are two special exceptions, seven pairs and thirteen orphans, covered later."),
                ),
            ),
        ),
        GuideSection(
            "The tiles",
            "There are 136 tiles in the set: 34 different tiles, four copies of each.",
            listOf(
                GuideCard(
                    "Number tiles",
                    "Three suits, each numbered 1 to 9. The names come from the Japanese words for what's printed on them.",
                    examples = listOf(
                        TileExample("Man (characters)", "123456789m"),
                        TileExample("Pin (circles)", "123456789p"),
                        TileExample("Sou (bamboo)", "123456789s"),
                    ),
                    notes = listOf("The 1s and 9s are called terminals. The 2s through 8s are called simples."),
                ),
                GuideCard(
                    "Honor tiles",
                    "Seven tiles that have no number and never form sequences. They only work as triplets or pairs.",
                    examples = listOf(
                        TileExample("Winds: East, South, West, North", "1234z"),
                        TileExample("Dragons: White, Green, Red", "567z"),
                    ),
                    notes = listOf("Terminals and honors together are called the 'yaochuu' tiles. You'll see them in many yaku."),
                ),
                GuideCard(
                    "Red fives",
                    "Most sets swap one regular 5 in each number suit for a red 5. They play exactly like any other 5, " +
                        "but each one in your finished hand is worth a bonus han (see 'Bonus tiles').",
                    examples = listOf(TileExample("Red 5 man, pin and sou", "0m 0p 0s")),
                ),
            ),
        ),
        GuideSection(
            "Melds and the pair",
            "Tiles are grouped into sets. Learning to see these groups is most of the game.",
            listOf(
                GuideCard(
                    "The three kinds of meld",
                    table = GuideTable(
                        listOf("Meld", "What it is"),
                        listOf(
                            listOf("Sequence (shuntsu)", "Three consecutive numbers in the same suit."),
                            listOf("Triplet (koutsu)", "Three identical tiles."),
                            listOf("Quad (kantsu)", "Four identical tiles. It counts as one meld."),
                        ),
                        textual = true,
                    ),
                    examples = listOf(
                        TileExample("Sequence", "345p"),
                        TileExample("Triplet", "888s"),
                        TileExample("Quad", "7777m"),
                    ),
                    notes = listOf("Sequences must stay within one suit and can't wrap around, so 8-9-1 is not a sequence."),
                ),
                GuideCard(
                    "The pair",
                    "Two identical tiles. Every normal winning hand needs exactly one, and it's the only set in the hand that is not three tiles.",
                    examples = listOf(TileExample("A pair", "22m")),
                ),
                GuideCard(
                    "Closed or open",
                    "A meld you make entirely from tiles you drew yourself is concealed. A meld you complete by taking " +
                        "another player's discard is open, and is shown face up on the table. A hand with no open melds is " +
                        "called closed, and many strong yaku are only available to closed hands.",
                ),
            ),
        ),
        GuideSection(
            "Needing a yaku",
            "The rule that separates riichi from other mahjong games.",
            listOf(
                GuideCard(
                    "What a yaku is",
                    "A yaku is a named scoring pattern, like 'all simples' or 'a triplet of dragons'. Having a complete " +
                        "hand is not enough to win. Your hand must also contain at least one yaku. Think of it as the " +
                        "reason your hand is allowed to win.",
                    bullets = listOf(
                        "A complete hand with a yaku can be declared a win.",
                        "A complete hand with no yaku is worth nothing, and you can't win with it.",
                        "Bonus tiles (dora) never count as a yaku. They only add points to a hand that already has one.",
                    ),
                ),
                GuideCard(
                    "Same shape, different result",
                    examples = listOf(
                        TileExample("Has a yaku: every tile is a simple (2 to 8), so this is 'tanyao'", "234m 567p 345s 678s 44p"),
                        TileExample("No yaku by itself: complete, but nothing here qualifies", "123m 456p 888s 234s 99p"),
                    ),
                    notes = listOf(
                        "The second hand can still win if you add a yaku, for example by declaring riichi while it's closed.",
                    ),
                ),
                GuideCard(
                    "Easy yaku to start with",
                    bullets = listOf(
                        "Riichi: declare that your closed hand is one tile from winning.",
                        "Menzen tsumo: win on a tile you drew yourself with a closed hand.",
                        "Tanyao: a hand made only of the numbers 2 to 8.",
                        "Yakuhai: a triplet of dragons, or of a wind that matters this round.",
                    ),
                    notes = listOf("Every yaku, and how much it's worth, is in the Yaku List."),
                ),
            ),
        ),
        GuideSection(
            "How a round is played",
            "From dealing the tiles to someone winning.",
            listOf(
                GuideCard(
                    "Setting up",
                    bullets = listOf(
                        "Players sit at the four compass points. The player in the East seat is the dealer.",
                        "The tiles are shuffled and stacked into a wall. The last 14 tiles are set aside as the dead wall.",
                        "Everyone is dealt 13 tiles. The dealer starts with a 14th by drawing first.",
                        "The top tile of the dead wall is turned over as the dora indicator (see 'Bonus tiles').",
                    ),
                ),
                GuideCard(
                    "Your turn",
                    "Play moves counter-clockwise, starting with the dealer.",
                    bullets = listOf(
                        "Draw one tile from the wall. You now have 14 tiles.",
                        "If those 14 tiles are a complete hand with a yaku, you can declare a win by tsumo.",
                        "Otherwise, discard one tile face up in front of you. You're back to 13.",
                        "Discards stay on the table in the order they were thrown, so everyone can see them.",
                    ),
                ),
                GuideCard(
                    "Winning",
                    "There are two ways to win, and both need a complete hand with a yaku.",
                    table = GuideTable(
                        listOf("Win", "How it happens"),
                        listOf(
                            listOf("Tsumo", "You draw the tile that completes your hand."),
                            listOf("Ron", "Another player discards the tile that completes your hand, and you take it to win."),
                        ),
                        textual = true,
                    ),
                    notes = listOf("On a ron, the player who discarded the winning tile pays. On a tsumo, everyone pays."),
                ),
            ),
        ),
        GuideSection(
            "Calling tiles",
            "You don't have to wait for the wall. You can take another player's discard to finish a meld.",
            listOf(
                GuideCard(
                    "The calls",
                    table = GuideTable(
                        listOf("Call", "What you do"),
                        listOf(
                            listOf("Chi", "Take the discard to make a sequence. Only allowed from the player just before you."),
                            listOf("Pon", "Take the discard to make a triplet. Allowed from any player."),
                            listOf("Kan", "Make a quad, from a discard, from four tiles in your hand, or by adding to a triplet. You draw a replacement tile from the dead wall."),
                            listOf("Ron", "Take the discard to win."),
                        ),
                        textual = true,
                    ),
                    notes = listOf(
                        "After a chi or pon you show the meld, then discard a tile. Play carries on from you, skipping anyone in between.",
                        "If several players want the same discard, ron beats pon or kan, which beat chi.",
                    ),
                ),
                GuideCard(
                    "The cost of calling",
                    "Calling gets you melds faster, but it makes your hand open. That lowers the value of some yaku and rules " +
                        "out others entirely, such as riichi. Calling is also a tell: everyone can see what you're collecting. " +
                        "A concealed kan, made from four tiles you drew yourself, does not open your hand.",
                ),
            ),
        ),
        GuideSection(
            "Riichi and getting ready",
            "The move the game is named after.",
            listOf(
                GuideCard(
                    "Tenpai: one tile away",
                    "When your 13 tiles need just one more tile to become a complete hand, you're 'tenpai' (ready). " +
                        "The tiles that would finish it are your 'waits'.",
                    examples = listOf(TileExample("Tenpai: three melds, a pair and two linked tiles. Waiting on 2 or 5 circles", "234m 567p 789s 11z 34p")),
                ),
                GuideCard(
                    "Declaring riichi",
                    "If you're tenpai and your hand is closed, you can declare riichi: say so aloud, put 1,000 points on " +
                        "the table as a stake, and place your discard sideways.",
                    bullets = listOf(
                        "Riichi is itself a yaku, so it makes almost any closed ready hand a valid winner.",
                        "After declaring, you can no longer change your hand. You draw and discard the tile you drew, until you win.",
                        "A riichi winner also gets to look under the dora indicators for extra bonus tiles.",
                    ),
                    notes = listOf("If someone else wins before you, you lose the 1,000 stake."),
                ),
                GuideCard(
                    "Furiten: the one rule everyone trips on",
                    "If a tile that would complete your hand is in your own discard pile, you can't win by ron on any " +
                        "tile. That includes a winning tile you passed on earlier, until your next turn. You can still win by tsumo.",
                    notes = listOf("The scoring screen can't see your discards, so watch for this one yourself."),
                ),
            ),
        ),
        GuideSection(
            "Bonus tiles",
            "Extra han that make a hand worth more. They never count as a yaku.",
            listOf(
                GuideCard(
                    "Dora",
                    "The revealed dora indicator is not the bonus tile. The next tile in order is. Each dora tile in your " +
                        "final hand is worth one extra han.",
                    table = GuideTable(
                        listOf("Indicator", "Dora"),
                        listOf(
                            listOf("Any number 1 to 8", "The next number up, in the same suit"),
                            listOf("9 of a suit", "The 1 of that suit"),
                            listOf("East, South, West, North", "The next wind, and North goes back to East"),
                            listOf("White, Green, Red dragons", "The next dragon, and Red goes back to White"),
                        ),
                        textual = true,
                    ),
                ),
                GuideCard(
                    "Ura dora and red fives",
                    bullets = listOf(
                        "Ura dora: tiles under the dora indicators, turned over only for a riichi winner. Each is worth one han.",
                        "Red fives: each one in your hand is worth one han, like a dora.",
                        "Kan: each kan reveals another dora indicator.",
                    ),
                ),
            ),
        ),
        GuideSection(
            "Ending a round and a game",
            "How a round finishes and how the game moves on.",
            listOf(
                GuideCard(
                    "When someone wins",
                    "The round ends and points change hands based on the hand's value (see the Point Calculation Guide). " +
                        "If the dealer won, they stay dealer. Otherwise the dealer role passes to the next player.",
                ),
                GuideCard(
                    "When nobody wins",
                    "If the wall runs out, the round is a draw. Players who are tenpai show their hands, and the players who " +
                        "aren't pay them a total of 3,000 points between them. The dealer keeps the dealer role if they're tenpai.",
                ),
                GuideCard(
                    "Rounds and winds",
                    "Everyone starts with 25,000 points. A standard game has an East round and then a South round, each " +
                        "lasting until every player has been dealer once, and ends after the last of those. Your seat wind is " +
                        "your position relative to the dealer, and the round wind is East in the first round and South in the second.",
                    notes = listOf(
                        "A triplet of your seat wind or of the round wind is a yakuhai, a yaku. A wind that is both counts twice.",
                        "Each time the dealer keeps their role, a repeat counter called honba goes up, adding a small bonus to every win.",
                    ),
                ),
            ),
        ),
        GuideSection(
            "Special hands",
            "Two ways to win that skip the four-melds-and-a-pair shape.",
            listOf(
                GuideCard(
                    "Seven pairs (chiitoitsu)",
                    "Seven different pairs. It's a yaku by itself, worth 2 han, and only available to a closed hand.",
                    examples = listOf(TileExample("Seven pairs", "11m 55m 33p 99p 22s 77s 44z")),
                ),
                GuideCard(
                    "Thirteen orphans (kokushi musou)",
                    "One of each terminal and honor tile, plus one extra of any of them. It's a yakuman, the biggest hand there is.",
                    examples = listOf(TileExample("Thirteen orphans", "19m 19p 19s 1234567z 1z")),
                ),
            ),
        ),
        GuideSection(
            "Your first game",
            "A few habits that help a lot while you're learning.",
            listOf(
                GuideCard(
                    "Tips for a first game",
                    bullets = listOf(
                        "Aim for a simple hand of 2s to 8s. You'll get tanyao almost for free.",
                        "Stay closed where you can, so riichi stays available.",
                        "Hold pairs of dragons or valuable winds. A third copy gives you a yaku right away.",
                        "Watch what others discard. A tile that's been thrown many times is usually safe to throw.",
                        "When a rule is unclear, use this page. When the score is unclear, use the Point Calculation Guide.",
                    ),
                ),
                GuideCard(
                    "Quick glossary",
                    table = GuideTable(
                        listOf("Term", "Meaning"),
                        listOf(
                            listOf("Meld", "A set of tiles: a sequence, triplet or quad."),
                            listOf("Yaku", "A named scoring pattern. You need at least one to win."),
                            listOf("Han", "Scoring points for yaku and bonus tiles."),
                            listOf("Fu", "Extra points for the hand's makeup and how it was won."),
                            listOf("Tenpai", "One tile away from a complete hand."),
                            listOf("Riichi", "Declaring a ready, closed hand for 1,000 points."),
                            listOf("Tsumo", "Winning on a tile you drew yourself."),
                            listOf("Ron", "Winning on another player's discard."),
                            listOf("Dora", "Bonus tiles worth one han each."),
                        ),
                        textual = true,
                    ),
                ),
            ),
        ),
    )
}
