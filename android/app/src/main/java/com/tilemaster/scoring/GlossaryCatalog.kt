package com.tilemaster.scoring

import com.tilemaster.model.Tile

enum class GlossaryCategory(val title: String, val blurb: String) {
    TILES("Tiles", "The pieces you play with."),
    SETS("Melds and hands", "How tiles group together."),
    READY("Getting ready", "Being close to winning, and what you're waiting for."),
    ACTIONS("Actions", "Things you do on a turn."),
    SCORING("Scoring", "Words you'll see on the score screen."),
    GAME("Rounds and seats", "How a game is organised."),
}

/**
 * One glossary term. [aliases] are other names people use, like the Japanese word, and are searched
 * as strongly as the term itself. [example] is optional tiles in standard notation (see [parseTiles]).
 */
data class GlossaryTerm(
    val term: String,
    val category: GlossaryCategory,
    val meaning: String,
    val description: String,
    val aliases: List<String> = emptyList(),
    val example: String? = null,
) {
    val exampleGroups: List<List<Tile>> get() = example?.let(::parseTiles).orEmpty()
}

object GlossaryCatalog {

    private val T = GlossaryCategory.TILES
    private val S = GlossaryCategory.SETS
    private val R = GlossaryCategory.READY
    private val A = GlossaryCategory.ACTIONS
    private val P = GlossaryCategory.SCORING
    private val G = GlossaryCategory.GAME

    val terms: List<GlossaryTerm> = listOf(
        // Tiles
        GlossaryTerm(
            "Suit", T, "Man, pin or sou",
            "The three groups of numbered tiles. Man are characters, pin are circles and sou are bamboo. " +
                "Each has the numbers 1 to 9.",
            aliases = listOf("man", "pin", "sou", "characters", "circles", "bamboo"),
            example = "123m 123p 123s",
        ),
        GlossaryTerm(
            "Terminal", T, "A 1 or a 9",
            "The end tiles of each number suit. Many yaku need them, and many others forbid them.",
            aliases = listOf("routouhai"),
            example = "19m 19p 19s",
        ),
        GlossaryTerm(
            "Simple", T, "A number from 2 to 8",
            "Any number tile that isn't a terminal. A hand of only simples is the tanyao yaku.",
            aliases = listOf("chunchan"),
            example = "2345678p",
        ),
        GlossaryTerm(
            "Honor", T, "A wind or a dragon",
            "The seven tiles with no number. They can only be used in triplets or pairs, never sequences.",
            aliases = listOf("jihai", "word tile"),
            example = "1234z 567z",
        ),
        GlossaryTerm(
            "Wind", T, "East, South, West or North",
            "Four honor tiles. A triplet of a wind that matches your seat or the round is a yaku.",
            aliases = listOf("kazehai", "east", "south", "west", "north"),
            example = "1234z",
        ),
        GlossaryTerm(
            "Dragon", T, "White, Green or Red",
            "Three honor tiles. A triplet of any dragon is a yaku, always.",
            aliases = listOf("sangenpai", "haku", "hatsu", "chun", "white", "green", "red"),
            example = "567z",
        ),
        GlossaryTerm(
            "Red five", T, "A special 5 worth a bonus han",
            "One 5 in each number suit is painted red. It plays like any other 5, but is worth one han " +
                "if it's in your winning hand.",
            aliases = listOf("aka dora", "red dora"),
            example = "0m 0p 0s",
        ),
        GlossaryTerm(
            "Wall", T, "The face-down tiles you draw from",
            "All the tiles are stacked into a wall before the deal. Players take turns drawing from it.",
            aliases = listOf("yama", "live wall"),
        ),
        GlossaryTerm(
            "Dead wall", T, "14 tiles set aside",
            "The last 14 tiles of the wall. They hold the dora indicators and the replacement tiles drawn after a kan, " +
                "and are never drawn from normally.",
            aliases = listOf("wanpai"),
        ),

        // Melds and hands
        GlossaryTerm(
            "Meld", S, "A set of tiles",
            "A sequence, a triplet or a quad. A normal winning hand is four melds and a pair.",
            aliases = listOf("mentsu", "set", "group"),
        ),
        GlossaryTerm(
            "Sequence", S, "Three in a row",
            "Three consecutive numbers in one suit, like 3-4-5 circles. Sequences can't wrap around, and honors can't be in them.",
            aliases = listOf("shuntsu", "run", "chi"),
            example = "345p",
        ),
        GlossaryTerm(
            "Triplet", S, "Three of a kind",
            "Three identical tiles.",
            aliases = listOf("koutsu", "pon"),
            example = "888s",
        ),
        GlossaryTerm(
            "Quad", S, "Four of a kind",
            "Four identical tiles, made by calling kan. It counts as a single meld, and you draw an extra tile for it.",
            aliases = listOf("kantsu", "kan"),
            example = "7777m",
        ),
        GlossaryTerm(
            "Pair", S, "Two identical tiles",
            "Every normal winning hand needs exactly one pair alongside its four melds.",
            aliases = listOf("jantou", "atama", "eyes", "head"),
            example = "22m",
        ),
        GlossaryTerm(
            "Closed hand", S, "No tiles taken from discards",
            "A hand that has no open melds. Riichi and several other yaku need a closed hand.",
            aliases = listOf("menzen", "concealed"),
        ),
        GlossaryTerm(
            "Open hand", S, "Contains a called meld",
            "A hand with at least one meld made from another player's discard. It's easier to build but scores less.",
            aliases = listOf("fuuro", "naki", "exposed"),
        ),

        // Getting ready
        GlossaryTerm(
            "Tenpai", R, "One tile from winning",
            "Your 13 tiles need exactly one more tile to be a complete hand. You must be tenpai to declare riichi.",
            aliases = listOf("ready", "ready hand", "waiting"),
            example = "234m 567p 789s 11z 34p",
        ),
        GlossaryTerm(
            "Noten", R, "Not ready",
            "A hand that is not tenpai. At a drawn round, players who are noten pay those who are tenpai.",
            aliases = listOf("not ready"),
        ),
        GlossaryTerm(
            "Shanten", R, "How far from tenpai",
            "The number of tile swaps needed to become tenpai. A hand one swap away is 1-shanten, and tenpai is 0-shanten.",
        ),
        GlossaryTerm(
            "Wait", R, "The tiles that would win",
            "The tile or tiles that would complete your tenpai hand. The shape of the wait also affects fu.",
            aliases = listOf("machi", "waits"),
        ),
        GlossaryTerm(
            "Two-sided wait", R, "Waiting on either end of a run",
            "Two tiles in a row that can be completed on both sides. It's the best wait, since it has the most winning tiles.",
            aliases = listOf("ryanmen", "open wait"),
            example = "45p",
        ),
        GlossaryTerm(
            "Closed wait", R, "Waiting on the middle",
            "Two tiles with a gap between them, which only one tile can fill. It's worth 2 fu.",
            aliases = listOf("kanchan", "middle wait", "gap wait"),
            example = "46p",
        ),
        GlossaryTerm(
            "Edge wait", R, "Waiting on the edge of a run",
            "A 1-2 waiting on 3, or an 8-9 waiting on 7. Only one tile completes it. It's worth 2 fu.",
            aliases = listOf("penchan"),
            example = "12p",
        ),
        GlossaryTerm(
            "Pair wait", R, "Waiting to make the pair",
            "Your other 13 tiles are complete and you hold one extra tile, waiting for a second copy. It's worth 2 fu.",
            aliases = listOf("tanki", "single wait"),
            example = "5p",
        ),
        GlossaryTerm(
            "Furiten", R, "You can't win by ron",
            "If a tile that would win for you is in your own discards, you can't win by ron on any tile, only by tsumo. " +
                "Passing on a winning tile has the same effect until your next turn.",
                    ),
        GlossaryTerm(
            "Riichi", R, "Declaring a ready closed hand",
            "Say 'riichi' and stake 1,000 points to commit to a closed tenpai hand. It's a yaku, but your hand can't change afterwards.",
            aliases = listOf("reach", "richi"),
        ),

        // Actions
        GlossaryTerm(
            "Discard", A, "Put a tile in the pile",
            "After drawing, you throw away one tile face up, back to 13. Everyone can see the pile.",
            aliases = listOf("throw", "dahai", "river", "pond"),
        ),
        GlossaryTerm(
            "Call", A, "Take another player's discard",
            "Chi, pon, kan or ron on a discard. Everything except ron makes your hand open.",
            aliases = listOf("naki", "steal"),
        ),
        GlossaryTerm(
            "Chi", A, "Call for a sequence",
            "Take the discard of the player just before you to make a sequence. You can't chi from anyone else.",
            aliases = listOf("chii", "chow"),
        ),
        GlossaryTerm(
            "Pon", A, "Call for a triplet",
            "Take a discard from any player to make a triplet. It beats chi if both are wanted.",
            aliases = listOf("peng", "pung"),
        ),
        GlossaryTerm(
            "Kan", A, "Make a quad",
            "Make four of a kind, from a discard, from four tiles in your hand, or by adding the fourth tile to a triplet. " +
                "You draw a replacement tile, and a new dora indicator is turned over.",
            aliases = listOf("quad", "daiminkan", "ankan", "shouminkan"),
        ),
        GlossaryTerm(
            "Tsumo", A, "Win on your own draw",
            "Winning with a tile you drew from the wall. Everyone pays you. With a closed hand, it's a yaku on its own.",
            aliases = listOf("self-draw", "self draw"),
        ),
        GlossaryTerm(
            "Ron", A, "Win on a discard",
            "Winning with a tile another player discarded. The player who discarded it pays you.",
            aliases = listOf("deal in", "discard win"),
        ),

        // Scoring
        GlossaryTerm(
            "Yaku", P, "A scoring pattern",
            "A named pattern that makes a hand valid. You need at least one yaku to win. Dora don't count.",
            aliases = listOf("pattern", "condition"),
        ),
        GlossaryTerm(
            "Yakuhai", P, "Valuable honor triplet",
            "A triplet of dragons, your seat wind or the round wind. Each is a yaku worth one han.",
            aliases = listOf("value tiles", "honor triplet"),
            example = "555z",
        ),
        GlossaryTerm(
            "Yakuman", P, "The biggest hands",
            "A rare hand worth a fixed, very large score regardless of han or fu. A dealer's yakuman pays 48,000.",
            aliases = listOf("limit hand", "full limit"),
        ),
        GlossaryTerm(
            "Han", P, "Scoring doublings",
            "Each yaku and bonus tile is worth han. Every extra han roughly doubles the hand's value.",
            aliases = listOf("double", "fan"),
        ),
        GlossaryTerm(
            "Fu", P, "Minipoints",
            "Points for a hand's makeup and how it was won. Fu are added up, rounded to 10, and multiplied into the score.",
            aliases = listOf("minipoints", "mini points"),
        ),
        GlossaryTerm(
            "Mangan", P, "The first limit",
            "A hand worth 5 han, or 4 han at 40 fu or more, or 3 han at 70 fu or more. It's 8,000 for a non-dealer and 12,000 for the dealer.",
            aliases = listOf("limit", "haneman", "baiman"),
        ),
        GlossaryTerm(
            "Dora", P, "A bonus tile",
            "Each dora in your winning hand adds one han. It never counts as a yaku, so you still need one to win.",
            aliases = listOf("bonus tile", "bonus"),
        ),
        GlossaryTerm(
            "Dora indicator", P, "The tile that picks the dora",
            "The revealed tile on the table. The dora is the next tile in order. After 9 comes 1, North goes to East and Red dragon goes to White.",
            aliases = listOf("indicator", "omote dora"),
        ),
        GlossaryTerm(
            "Ura dora", P, "Hidden bonus tiles",
            "Tiles under the dora indicators, turned over only for a player who won with riichi. Each is worth one han.",
            aliases = listOf("under dora", "hidden dora"),
        ),
        GlossaryTerm(
            "Honba", P, "Repeat counter",
            "Goes up each time the dealer wins again or a round ends in a draw. Each honba adds 300 points to the winner's payout.",
            aliases = listOf("counter", "repeat"),
        ),
        GlossaryTerm(
            "Riichi stick", P, "The 1,000 point stake",
            "The 1,000 points put on the table when declaring riichi. The next player to win takes every stick on the table.",
            aliases = listOf("kyoutaku", "deposit", "stake"),
        ),

        // Rounds and seats
        GlossaryTerm(
            "Dealer", G, "The East seat",
            "The player who goes first in a round. The dealer wins and loses 50% more points, and stays dealer after a win.",
            aliases = listOf("oya", "east player", "host"),
        ),
        GlossaryTerm(
            "Seat wind", G, "Your own wind",
            "The wind of your seat, starting with East for the dealer. A triplet of it is a yaku.",
            aliases = listOf("jikaze", "player wind"),
        ),
        GlossaryTerm(
            "Round wind", G, "The wind of the round",
            "East in the first round of a game and South in the second. A triplet of it is a yaku for every player.",
            aliases = listOf("bakaze", "prevalent wind"),
        ),
        GlossaryTerm(
            "Round", G, "One hand played from deal to finish",
            "From dealing the tiles until someone wins or the wall runs out. A game is a series of rounds.",
            aliases = listOf("kyoku", "hand"),
        ),
        GlossaryTerm(
            "Hanchan", G, "A standard full game",
            "An East round followed by a South round. A shorter game of only the East round is called a tonpuusen.",
            aliases = listOf("game", "match", "tonpuusen"),
        ),
        GlossaryTerm(
            "Exhaustive draw", G, "Nobody won",
            "The wall ran out. Players who are tenpai split 3,000 points from those who aren't.",
            aliases = listOf("ryuukyoku", "draw", "wall exhausted"),
        ),
        GlossaryTerm(
            "Dealer repeat", G, "The dealer stays",
            "If the dealer wins, or is tenpai at a drawn round, they stay dealer and the honba counter goes up.",
            aliases = listOf("renchan", "stay dealer"),
        ),
    )
}
