package com.tilemaster.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tilemaster.model.Tile
import com.tilemaster.scoring.HandContext
import com.tilemaster.scoring.MAX_DORA_INDICATORS
import com.tilemaster.scoring.Meld
import com.tilemaster.scoring.MeldType
import com.tilemaster.scoring.QuestionStep
import com.tilemaster.scoring.Riichi
import com.tilemaster.scoring.Wind

@Composable
fun QuestionsScreen(
    step: QuestionStep,
    tiles: List<Tile>,
    context: HandContext,
    isProcessing: Boolean,
    onUpdate: ((HandContext) -> HandContext) -> Unit,
    onAddMeld: (MeldType, List<Int>) -> String?,
    onRemoveMeld: (Meld) -> Unit,
    onScanIndicators: (ura: Boolean) -> Unit,
    onGoToStep: (QuestionStep) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = QuestionStep.relevant(context)
    val position = steps.indexOf(step) + 1
    val blocker = step.blocker(context, tiles)

    Column(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text(
                    "Step $position of ${steps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        LinearProgressIndicator(
            progress = { position.toFloat() / steps.size },
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                QuestionStep.MELDS -> MeldsStep(tiles, context, onAddMeld, onRemoveMeld)
                QuestionStep.WINNING_TILE -> WinningTileStep(tiles, context, onUpdate)
                QuestionStep.WIN_TYPE -> WinTypeStep(context, onUpdate)
                QuestionStep.WINDS -> WindsStep(context, onUpdate)
                QuestionStep.RIICHI -> RiichiStep(context, onUpdate)
                QuestionStep.DORA -> IndicatorsStep(
                    indicators = context.doraIndicators,
                    lead = "Add the face-up dora indicators from the dead wall.",
                    tips = listOf(
                        "The tile after each indicator is dora.",
                        "Red fives in your hand count automatically.",
                    ),
                    onChange = { list -> onUpdate { it.copy(doraIndicators = list) } },
                    onScan = { onScanIndicators(false) },
                )
                QuestionStep.URA_DORA -> IndicatorsStep(
                    indicators = context.uraDoraIndicators,
                    lead = "Flip the tiles under the dora indicators and add them here.",
                    tips = listOf("Only riichi winners get ura dora."),
                    onChange = { list -> onUpdate { it.copy(uraDoraIndicators = list) } },
                    onScan = { onScanIndicators(true) },
                )
                QuestionStep.SPECIAL -> SpecialStep(context, onUpdate)
                QuestionStep.TABLE -> TableStep(context, onUpdate)
                QuestionStep.SUMMARY -> SummaryStep(tiles, context, onGoToStep)
            }
        }

        if (blocker != null && step == QuestionStep.SUMMARY) {
            Text(blocker, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onNext,
            enabled = blocker == null && !isProcessing,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (isProcessing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (step == QuestionStep.SUMMARY) "Score hand" else "Next")
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ---- Steps ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeldsStep(
    tiles: List<Tile>,
    context: HandContext,
    onAddMeld: (MeldType, List<Int>) -> String?,
    onRemoveMeld: (Meld) -> Unit,
) {
    var selection by remember(tiles) { mutableStateOf(emptyList<Int>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val inMeld = context.meldIndices

    Intro(
        lead = "Mark each chi, pon or kan you called.",
        steps = listOf("Tap the tiles of one meld.", "Choose what it was below."),
        tips = listOf("For a kan, select only the face-up tiles. The hidden ones are added for you."),
    )
    TileRow(
        tiles = tiles,
        isSelected = { it in selection },
        isDimmed = { it in inMeld },
        onClick = { index ->
            if (index !in inMeld) {
                error = null
                selection = if (index in selection) selection - index else selection + index
            }
        },
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MeldType.entries.forEach { type ->
            OutlinedButton(
                onClick = {
                    error = onAddMeld(type, selection)
                    if (error == null) selection = emptyList()
                },
                enabled = selection.isNotEmpty(),
            ) {
                Text(type.label)
            }
        }
    }
    error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

    if (context.melds.isNotEmpty()) {
        SectionLabel("Your calls")
        Tip(if (context.isOpen) "Your hand is open." else "Closed kans keep your hand closed.")
        context.melds.forEach { meld ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(meld.type.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(88.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        meldTiles(tiles, meld).forEach { TileFace(it, width = 28.dp) }
                    }
                    IconButton(onClick = { onRemoveMeld(meld) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove ${meld.type.label}")
                    }
                }
            }
        }
    } else {
        Tip("No calls marked, so your hand is closed. If that's right, tap Next.")
    }
}

@Composable
private fun WinningTileStep(tiles: List<Tile>, context: HandContext, onUpdate: ((HandContext) -> HandContext) -> Unit) {
    val inMeld = context.meldIndices
    Intro(
        lead = "Tap the tile that completed your hand.",
        tips = if (inMeld.isEmpty()) emptyList() else listOf("Greyed-out tiles are in called melds, so they can't be the winning tile."),
    )
    TileRow(
        tiles = tiles,
        isSelected = { it == context.winIndex },
        isDimmed = { it in inMeld },
        onClick = { index -> if (index !in inMeld) onUpdate { it.copy(winIndex = index) } },
    )
    context.winIndex?.let { tiles.getOrNull(it) }?.let {
        Text("Won on: ${it.displayName}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WinTypeStep(context: HandContext, onUpdate: ((HandContext) -> HandContext) -> Unit) {
    OptionCard(
        title = "Tsumo",
        subtitle = "You drew the winning tile yourself",
        selected = context.isTsumo == true,
        onClick = { onUpdate { it.copy(isTsumo = true) } },
    )
    OptionCard(
        title = "Ron",
        subtitle = "You won on another player's discard",
        selected = context.isTsumo == false,
        onClick = { onUpdate { it.copy(isTsumo = false) } },
    )
}

@Composable
private fun WindsStep(context: HandContext, onUpdate: ((HandContext) -> HandContext) -> Unit) {
    SectionLabel("Your seat wind")
    WindPicker(selected = context.seatWind, onPick = { wind -> onUpdate { it.copy(seatWind = wind) } })
    Tip(if (context.isDealer) "You're the dealer, so you score 1.5x." else "East is the dealer.")
    SectionLabel("Round wind")
    WindPicker(selected = context.roundWind, onPick = { wind -> onUpdate { it.copy(roundWind = wind) } })
}

@Composable
private fun RiichiStep(context: HandContext, onUpdate: ((HandContext) -> HandContext) -> Unit) {
    OptionCard(
        title = Riichi.NONE.label,
        subtitle = null,
        selected = context.riichi == Riichi.NONE,
        onClick = { onUpdate { it.copy(riichi = Riichi.NONE) } },
    )
    OptionCard(
        title = Riichi.RIICHI.label,
        subtitle = "You declared riichi",
        selected = context.riichi == Riichi.RIICHI,
        onClick = { onUpdate { it.copy(riichi = Riichi.RIICHI) } },
    )
    OptionCard(
        title = Riichi.DOUBLE.label,
        subtitle = "Declared on your first discard, before any calls",
        selected = context.riichi == Riichi.DOUBLE,
        onClick = { onUpdate { it.copy(riichi = Riichi.DOUBLE) } },
    )
    if (context.riichi != Riichi.NONE) {
        SwitchRow(
            title = "Ippatsu",
            subtitle = "Won within one go-around of riichi, with no calls in between",
            checked = context.isIppatsu,
            onChange = { v -> onUpdate { it.copy(isIppatsu = v) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndicatorsStep(
    indicators: List<Tile>,
    lead: String,
    tips: List<String>,
    onChange: (List<Tile>) -> Unit,
    onScan: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Intro(lead, tips = tips)
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                indicators.forEachIndexed { index, tile ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TileFace(tile, width = 40.dp, onClick = { onChange(indicators.filterIndexed { i, _ -> i != index }) })
                        Text(
                            "→ ${HandContext.doraOf(tile).displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (indicators.size < MAX_DORA_INDICATORS) AddTileSlot(onClick = { picking = true }, width = 40.dp)
            }
            Text(
                if (indicators.isEmpty()) "No indicators yet." else "Tap an indicator to remove it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text("Scan indicators with the camera")
    }
    if (picking) {
        TilePickerSheet(
            title = "Add an indicator",
            onPick = { onChange(indicators + it) },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun SpecialStep(context: HandContext, onUpdate: ((HandContext) -> HandContext) -> Unit) {
    Intro(lead = "Turn on any that apply.", tips = listOf("These are rare. Most hands have none."))
    var any = false
    if (context.canRinshan) {
        any = true
        SwitchRow("Rinshan kaihou", "You won on the replacement tile after a kan", context.isRinshan) { v ->
            onUpdate { it.copy(isRinshan = v, isHaitei = it.isHaitei && !v) }
        }
    }
    if (context.canHaitei) {
        any = true
        SwitchRow("Haitei", "You won on the last tile from the wall", context.isHaitei) { v ->
            onUpdate { it.copy(isHaitei = v, isRinshan = it.isRinshan && !v) }
        }
    }
    if (context.canChankan) {
        any = true
        SwitchRow("Chankan", "You won on a tile someone added to their pon to make a kan", context.isChankan) { v ->
            onUpdate { it.copy(isChankan = v, isHoutei = it.isHoutei && !v) }
        }
    }
    if (context.canHoutei) {
        any = true
        SwitchRow("Houtei", "You won on the last discard of the hand", context.isHoutei) { v ->
            onUpdate { it.copy(isHoutei = v, isChankan = it.isChankan && !v) }
        }
    }
    if (context.canTenhou) {
        any = true
        SwitchRow("Tenhou", "The dealer's starting hand was already complete", context.isTenhou) { v ->
            onUpdate { it.copy(isTenhou = v) }
        }
    }
    if (context.canChiihou) {
        any = true
        SwitchRow("Chiihou", "You won on your first draw, before anyone called", context.isChiihou) { v ->
            onUpdate { it.copy(isChiihou = v) }
        }
    }
    if (context.canRenhou) {
        any = true
        SwitchRow("Renhou", "You won on a discard before your first draw, before anyone called", context.isRenhou) { v ->
            onUpdate { it.copy(isRenhou = v) }
        }
    }
    if (!any) Tip("Nothing else applies to this hand.")
}

@Composable
private fun TableStep(context: HandContext, onUpdate: ((HandContext) -> HandContext) -> Unit) {
    Intro(lead = "Count the sticks on the table.")
    Stepper(
        title = "Honba",
        subtitle = "Repeat-hand counters. Each adds 300.",
        value = context.honba,
        onChange = { v -> onUpdate { it.copy(honba = v) } },
    )
    Stepper(
        title = "Riichi sticks",
        subtitle = "1,000 each, including yours. The winner takes them all.",
        value = context.riichiSticks,
        onChange = { v -> onUpdate { it.copy(riichiSticks = v) } },
    )
}

@Composable
private fun SummaryStep(tiles: List<Tile>, context: HandContext, onGoToStep: (QuestionStep) -> Unit) {
    TileRow(tiles = tiles, isSelected = { it == context.winIndex }, isDimmed = { false }, onClick = null)
    Tip("Tap a line to change it.")

    val winTile = context.winIndex?.let { tiles.getOrNull(it) }
    val special = listOfNotNull(
        "Rinshan".takeIf { context.isRinshan },
        "Haitei".takeIf { context.isHaitei },
        "Chankan".takeIf { context.isChankan },
        "Houtei".takeIf { context.isHoutei },
        "Tenhou".takeIf { context.isTenhou },
        "Chiihou".takeIf { context.isChiihou },
        "Renhou".takeIf { context.isRenhou },
    )
    Panel(padding = 4.dp) {
        Column {
            SummaryLine(
                "Calls",
                if (context.melds.isEmpty()) {
                    "None (closed hand)"
                } else {
                    context.melds.joinToString { "${it.type.label} ${tiles[it.indices.first()].displayName}" }
                },
            ) { onGoToStep(QuestionStep.MELDS) }
            SummaryLine("Winning tile", winTile?.displayName ?: "Not set") { onGoToStep(QuestionStep.WINNING_TILE) }
            SummaryLine("Win", if (context.isTsumo == true) "Tsumo" else "Ron") { onGoToStep(QuestionStep.WIN_TYPE) }
            SummaryLine(
                "Winds",
                "Seat ${context.seatWind?.label ?: "?"}${if (context.isDealer) " (dealer)" else ""} · Round ${context.roundWind.label}",
            ) { onGoToStep(QuestionStep.WINDS) }
            if (!context.isOpen) {
                SummaryLine(
                    "Riichi",
                    context.riichi.label + if (context.isIppatsu) " + ippatsu" else "",
                ) { onGoToStep(QuestionStep.RIICHI) }
            }
            SummaryLine("Dora indicators", context.doraIndicators.names()) { onGoToStep(QuestionStep.DORA) }
            if (context.riichi != Riichi.NONE) {
                SummaryLine("Ura dora indicators", context.uraDoraIndicators.names()) { onGoToStep(QuestionStep.URA_DORA) }
            }
            SummaryLine("Special", special.joinToString().ifEmpty { "None" }) { onGoToStep(QuestionStep.SPECIAL) }
            SummaryLine(
                "Table",
                "${context.honba} honba · ${context.riichiSticks} riichi stick${if (context.riichiSticks == 1) "" else "s"}",
            ) { onGoToStep(QuestionStep.TABLE) }
        }
    }
}

// ---- Building blocks ----

private fun List<Tile>.names() = joinToString { it.displayName }.ifEmpty { "None" }

/** The meld's tiles, including face-down kan tiles that weren't scanned. */
internal fun meldTiles(tiles: List<Tile>, meld: Meld): List<Tile> {
    val visible = meld.indices.map { tiles[it] }
    return visible + List(meld.hiddenCopies) { HandContext.plainTile(visible.first()) }
}

private const val ROW_COLUMNS = 8

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TileRow(
    tiles: List<Tile>,
    isSelected: (Int) -> Boolean,
    isDimmed: (Int) -> Boolean,
    onClick: ((Int) -> Unit)?,
) {
    Panel {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 4.dp
            val tileWidth: Dp = ((maxWidth - gap * (ROW_COLUMNS - 1)) / ROW_COLUMNS).coerceAtMost(44.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.forEachIndexed { index, tile ->
                    TileFace(
                        tile = tile,
                        width = tileWidth,
                        selected = isSelected(index),
                        onClick = onClick?.let { { it(index) } },
                        modifier = if (isDimmed(index)) Modifier.alpha(0.35f) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun Panel(padding: Dp = 14.dp, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(padding)) { content() }
    }
}

/** A step's instructions: one short lead sentence, optional numbered steps, then side notes. */
@Composable
private fun Intro(lead: String, steps: List<String> = emptyList(), tips: List<String> = emptyList()) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(lead, style = MaterialTheme.typography.bodyLarge)
        steps.forEachIndexed { index, step -> NumberedLine(index + 1, step) }
        tips.forEach { Tip(it) }
    }
}

@Composable
private fun NumberedLine(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A short side note, set apart from the main instructions by an info icon and muted color. */
@Composable
private fun Tip(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp).size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun OptionCard(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WindPicker(selected: Wind?, onPick: (Wind) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Wind.entries.forEach { wind ->
            val isSelected = wind == selected
            Surface(
                onClick = { onPick(wind) },
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TileFace(wind.tile, width = 32.dp)
                    Text(wind.label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun Stepper(title: String, subtitle: String, value: Int, onChange: (Int) -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > 0) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(40.dp),
            )
            FilledTonalIconButton(onClick = { onChange(value + 1) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
