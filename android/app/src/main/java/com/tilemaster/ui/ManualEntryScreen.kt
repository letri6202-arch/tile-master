package com.tilemaster.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tilemaster.model.Hand
import com.tilemaster.model.Suit
import com.tilemaster.model.Tile

/** Most tiles a hand can hold: 14 plus one per kan, up to four kans. */
private const val MAX_TILES = Hand.STANDARD_SIZE + 4

private const val COPIES_PER_TILE = 4

private val sections = listOf(
    "Characters" to Tile.entries.filter { it.suit == Suit.MAN && !it.isRedFive },
    "Circles" to Tile.entries.filter { it.suit == Suit.PIN && !it.isRedFive },
    "Bamboo" to Tile.entries.filter { it.suit == Suit.SOU && !it.isRedFive },
    "Honors" to Tile.entries.filter { it.suit == Suit.HONOR },
    "Red fives" to Tile.entries.filter { it.isRedFive },
)

/** Builds a hand by tapping tiles from a compact table of every tile, instead of scanning it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualEntryScreen(
    tiles: List<Tile>,
    onAddTile: (Tile) -> Unit,
    onRemoveTile: (index: Int) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Text("Enter hand", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Tap a tile below to add it, or one in your hand to remove it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        CurrentHand(tiles, onRemoveTile)

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            BoxWithConstraints(Modifier.padding(14.dp)) {
                val gap = 5.dp
                val tileWidth = ((maxWidth - gap * 8) / 9).coerceAtMost(40.dp)
                val full = tiles.size >= MAX_TILES
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                    sections.forEach { (name, sectionTiles) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                sectionTiles.forEach { tile ->
                                    // A red five counts toward the four copies of its plain five.
                                    val copies = tiles.count { it.suit == tile.suit && it.rank == tile.rank }
                                    val available = !full && if (tile.isRedFive) tile !in tiles else copies < COPIES_PER_TILE
                                    TileFace(
                                        tile,
                                        width = tileWidth,
                                        modifier = Modifier.alpha(if (available) 1f else 0.35f),
                                        onClick = if (available) ({ onAddTile(tile) }) else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HandStatus(Hand.problems(tiles))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            OutlinedButton(
                onClick = onClear,
                enabled = tiles.isNotEmpty(),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear")
            }
            Button(onClick = onNext, enabled = tiles.isNotEmpty(), modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Next")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentHand(tiles: List<Tile>, onRemoveTile: (Int) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Your hand · ${tiles.size}",
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
                val tileWidth = ((maxWidth - gap * 8) / 9).coerceAtMost(40.dp)
                if (tiles.isEmpty()) {
                    Text(
                        "No tiles yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(tileWidth * 1.36f + 8.dp),
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        tiles.forEachIndexed { index, tile ->
                            TileFace(tile, width = tileWidth, popIn = true, onClick = { onRemoveTile(index) })
                        }
                    }
                }
            }
        }
    }
}
