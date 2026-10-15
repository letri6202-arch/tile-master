package com.tilemaster.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tilemaster.scoring.GuideCard
import com.tilemaster.scoring.GuideSection
import com.tilemaster.scoring.GuideTable
import com.tilemaster.scoring.TileExample
import com.tilemaster.scoring.parseTiles

/** A guide page made of titled sections of cards, under a pinned back button. */
@Composable
fun GuidePage(
    title: String,
    intro: String,
    sections: List<GuideSection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        // Pinned above the list so it stays reachable however far the list is scrolled.
        GuideBackButton(onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        intro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            sections.forEach { section ->
                item(key = section.title) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            section.blurb,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(section.cards, key = { "${section.title}/${it.title}" }) { GuideCardView(it) }
            }
        }
    }
}

@Composable
private fun GuideCardView(card: GuideCard) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            card.body?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            card.formula?.let { Chip(it, highlighted = true) }
            card.examples.forEach { TileExampleView(it) }
            if (card.bullets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    card.bullets.forEach { Bullet(it) }
                }
            }
            card.table?.let { TableView(it) }
            card.notes.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Tile groups with a caption; spaces in the notation become small gaps between groups. */
@Composable
private fun TileExampleView(example: TileExample) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            example.caption,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            parseTiles(example.tiles).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    group.forEach { TileFace(it, width = 18.dp) }
                }
            }
        }
    }
}

/** The first column is the label and takes the most room; the rest are values. */
@Composable
private fun TableView(table: GuideTable) {
    val columns = table.rows.maxOf { it.size }
    Column {
        table.header?.let {
            TableRow(it, header = true, columns, table.textual)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        table.rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            TableRow(row, header = false, columns, table.textual)
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean, columns: Int, textual: Boolean) {
    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        cells.forEachIndexed { index, cell ->
            val first = index == 0
            // Numeric two-column tables split evenly so long values wrap sensibly; sentence tables
            // keep a narrow label column and give the text the room.
            val weight = when {
                textual -> if (first) 1f else 2.2f
                first -> if (columns == 2) 1f else 2f
                else -> 1f
            }
            Text(
                cell,
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (header || (first && textual) || (!first && !textual)) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = if (first || textual) TextAlign.Start else TextAlign.End,
                modifier = Modifier.weight(weight).align(Alignment.CenterVertically),
            )
        }
    }
}
