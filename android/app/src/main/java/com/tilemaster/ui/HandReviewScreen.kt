package com.tilemaster.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tilemaster.detection.Detection
import com.tilemaster.model.Hand
import com.tilemaster.model.Suit
import com.tilemaster.model.Tile
import kotlinx.coroutines.launch

/** What the tile picker is editing: an existing tile in the hand, or a new one to append. */
private sealed interface PickerTarget {
    data class Replace(val index: Int) : PickerTarget
    data object Add : PickerTarget
}

/** Tiles per rack row: 14 tiles plus the add slot fit in two rows. */
private const val RACK_COLUMNS = 8

@Composable
fun HandReviewScreen(
    photo: Bitmap,
    detections: List<Detection>,
    tiles: List<Tile>,
    onReplaceTile: (index: Int, tile: Tile) -> Unit,
    onRemoveTile: (index: Int) -> Unit,
    onAddTile: (Tile) -> Unit,
    onRetake: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by remember { mutableStateOf<PickerTarget?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = onRetake) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to camera")
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text("Review hand", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Tap a tile to fix it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MaterialTheme.shapes.large)
                .background(Color.Black),
        ) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "Captured hand",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            DetectionOverlay(detections, photo.width, photo.height, Modifier.fillMaxSize())
        }

        HandRack(
            tiles = tiles,
            selectedIndex = (picker as? PickerTarget.Replace)?.index,
            onTileClick = { picker = PickerTarget.Replace(it) },
            onAddClick = { picker = PickerTarget.Add },
        )

        HandStatus(Hand.problems(tiles))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f).height(52.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retake")
            }
            Button(onClick = onNext, enabled = tiles.isNotEmpty(), modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Next")
            }
        }
    }

    picker?.let { target ->
        TilePickerSheet(
            title = when (target) {
                is PickerTarget.Replace -> "Replace ${tiles.getOrNull(target.index)?.displayName ?: "tile"}"
                PickerTarget.Add -> "Add a tile"
            },
            showRemove = target is PickerTarget.Replace,
            onPick = { tile ->
                when (target) {
                    is PickerTarget.Replace -> onReplaceTile(target.index, tile)
                    PickerTarget.Add -> onAddTile(tile)
                }
            },
            onRemove = { if (target is PickerTarget.Replace) onRemoveTile(target.index) },
            onDismiss = { picker = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HandRack(
    tiles: List<Tile>,
    selectedIndex: Int?,
    onTileClick: (Int) -> Unit,
    onAddClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Hand",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    Hand.toNotation(tiles).ifEmpty { "—" },
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 4.dp
                val tileWidth = ((maxWidth - gap * (RACK_COLUMNS - 1)) / RACK_COLUMNS).coerceAtMost(44.dp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tiles.forEachIndexed { index, tile ->
                        TileFace(
                            tile = tile,
                            width = tileWidth,
                            popIn = true,
                            selected = selectedIndex == index,
                            onClick = { onTileClick(index) },
                        )
                    }
                    AddTileSlot(onClick = onAddClick, width = tileWidth)
                }
            }
        }
    }
}

@Composable
internal fun HandStatus(problems: List<String>) {
    val ok = problems.isEmpty()
    Surface(
        color = if (ok) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (ok) {
                    Text("Looks like a complete hand", style = MaterialTheme.typography.bodyLarge)
                } else {
                    problems.forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TilePickerSheet(
    title: String,
    showRemove: Boolean = false,
    onPick: (Tile) -> Unit,
    onRemove: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Animate the sheet away before removing it from composition.
    fun close(action: () -> Unit) {
        action()
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    val sections = listOf(
        "Characters" to Tile.entries.filter { it.suit == Suit.MAN && !it.isRedFive },
        "Circles" to Tile.entries.filter { it.suit == Suit.PIN && !it.isRedFive },
        "Bamboo" to Tile.entries.filter { it.suit == Suit.SOU && !it.isRedFive },
        "Honors" to Tile.entries.filter { it.suit == Suit.HONOR },
        "Red fives" to Tile.entries.filter { it.isRedFive },
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (showRemove) {
                    TextButton(onClick = { close(onRemove) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Size tiles so a full suit (9 tiles) fits on one line.
                val gap = 6.dp
                val tileWidth = ((maxWidth - gap * 8) / 9).coerceAtMost(40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    sections.forEach { (name, sectionTiles) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(gap),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                sectionTiles.forEach { tile ->
                                    TileFace(tile, width = tileWidth, onClick = { close { onPick(tile) } })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
