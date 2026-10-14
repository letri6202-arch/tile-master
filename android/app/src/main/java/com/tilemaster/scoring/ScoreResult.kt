package com.tilemaster.scoring

import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

data class YakuLine(val name: String, val han: Int, val isYakuman: Boolean) {
    val hanLabel: String
        get() = when {
            !isYakuman -> "$han han"
            han >= 26 -> "Double yakuman"
            else -> "Yakuman"
        }
}

data class FuLine(val fu: Int, val reason: String) {
    val label: String get() = FU_REASONS[reason] ?: reason.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** One row of who pays what. */
data class PaymentLine(val label: String, val points: Int)

data class ScoreResult(
    val han: Int,
    val fu: Int,
    val isTsumo: Boolean,
    val isOpenHand: Boolean,
    val isDealer: Boolean,
    val isYakuman: Boolean,
    /** The library's limit name: "", "mangan", "haneman", ..., "yakuman", "2x yakuman", "kazoe yakuman". */
    val limit: String,
    val yaku: List<YakuLine>,
    val fuDetails: List<FuLine>,
    val main: Int,
    val mainBonus: Int,
    val additional: Int,
    val additionalBonus: Int,
    val riichiSticks: Int,
    val total: Int,
) {
    /** "Mangan", "Double yakuman", or "3 han 40 fu" below a limit. */
    val headline: String
        get() = when {
            limit.isEmpty() -> "$han han $fu fu"
            limit.endsWith("x yakuman") -> MULTIPLE_YAKUMAN[limit.first()] ?: limit
            else -> limit.replaceFirstChar { it.uppercase() }
        }

    /** The han/fu line under a limit headline; null when the headline already says it. */
    val detail: String?
        get() = when {
            limit.isEmpty() -> null
            isYakuman -> null
            else -> "$han han $fu fu"
        }

    /** Payment in table shorthand: "8,000", "4,000 all", or "2,000 / 4,000" (non-dealers / dealer). */
    val callout: String
        get() = when {
            !isTsumo -> points(main + mainBonus)
            isDealer -> "${points(main + mainBonus)} all"
            else -> "${points(additional + additionalBonus)} / ${points(main + mainBonus)}"
        }

    val payments: List<PaymentLine>
        get() = buildList {
            when {
                !isTsumo -> add(PaymentLine("From the discarder", main + mainBonus))
                isDealer -> add(PaymentLine("From each player", main + mainBonus))
                else -> {
                    add(PaymentLine("From the dealer", main + mainBonus))
                    add(PaymentLine("From each non-dealer", additional + additionalBonus))
                }
            }
            if (riichiSticks > 0) add(PaymentLine("Riichi sticks on the table", riichiSticks))
        }

    /** Honba bonus included in [total], for a note under the payments. */
    val honbaBonus: Int
        get() = when {
            !isTsumo -> mainBonus
            isDealer -> mainBonus * 3
            else -> mainBonus + additionalBonus * 2
        }

    companion object {
        private val MULTIPLE_YAKUMAN = mapOf(
            '2' to "Double yakuman",
            '3' to "Triple yakuman",
            '4' to "Quadruple yakuman",
            '5' to "Quintuple yakuman",
            '6' to "Sextuple yakuman",
        )

        fun points(value: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(value)
    }
}

sealed interface ScoreOutcome {
    data class Scored(val result: ScoreResult) : ScoreOutcome

    /** The hand can't be scored; [message] tells the player why. */
    data class Failed(val message: String) : ScoreOutcome

    companion object {
        /** Parses the response JSON from `scorer.score`. */
        fun fromJson(json: String, isTsumo: Boolean): ScoreOutcome {
            val obj = JSONObject(json)
            if (!obj.getBoolean("ok")) return Failed(obj.getString("error"))

            val yaku = obj.getJSONArray("yaku").let { array ->
                List(array.length()) { i ->
                    val y = array.getJSONObject(i)
                    YakuLine(y.getString("name"), y.getInt("han"), y.getBoolean("is_yakuman"))
                }
            }
            val fuDetails = obj.getJSONArray("fu_details").let { array ->
                List(array.length()) { i ->
                    val f = array.getJSONObject(i)
                    FuLine(f.getInt("fu"), f.getString("reason"))
                }
            }
            val payment = obj.getJSONObject("payment")
            return Scored(
                ScoreResult(
                    han = obj.getInt("han"),
                    fu = obj.getInt("fu"),
                    isTsumo = isTsumo,
                    isOpenHand = obj.getBoolean("is_open_hand"),
                    isDealer = obj.getBoolean("is_dealer"),
                    isYakuman = obj.getBoolean("is_yakuman"),
                    limit = obj.getString("limit"),
                    yaku = yaku,
                    fuDetails = fuDetails,
                    main = payment.getInt("main"),
                    mainBonus = payment.getInt("main_bonus"),
                    additional = payment.getInt("additional"),
                    additionalBonus = payment.getInt("additional_bonus"),
                    riichiSticks = payment.getInt("riichi_sticks"),
                    total = payment.getInt("total"),
                ),
            )
        }
    }
}

private val FU_REASONS = mapOf(
    "base" to "Base",
    "penchan" to "Edge wait",
    "kanchan" to "Closed wait",
    "pair_wait" to "Pair wait",
    "valued_pair" to "Value pair",
    "double_valued_pair" to "Double value pair",
    "tsumo" to "Tsumo",
    "hand_without_fu" to "Open pinfu",
    "open_pon" to "Open triplet",
    "closed_pon" to "Closed triplet",
    "open_terminal_pon" to "Open terminal/honor triplet",
    "closed_terminal_pon" to "Closed terminal/honor triplet",
    "open_kan" to "Open kan",
    "closed_kan" to "Closed kan",
    "open_terminal_kan" to "Open terminal/honor kan",
    "closed_terminal_kan" to "Closed terminal/honor kan",
)
