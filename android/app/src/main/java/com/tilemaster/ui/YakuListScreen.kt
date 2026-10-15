package com.tilemaster.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tilemaster.scoring.YakuSearch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tilemaster.scoring.YakuCatalog
import com.tilemaster.scoring.YakuCategory
import com.tilemaster.scoring.YakuEntry
import com.tilemaster.scoring.YakuValue

@Composable
fun YakuListScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    // Re-ranked on every keystroke, so results appear as you type.
    val results = remember(query) { YakuSearch.search(YakuCatalog.entries, query) }

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        // Pinned above the list so both stay reachable however far the list is scrolled.
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuideBackButton(onBack)
            GuideSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search yaku",
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 20.dp, top = 4.dp),
            )
        }
        YakuListContent(query = query, results = results, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun YakuListContent(query: String, results: List<YakuEntry>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val searching = query.isNotBlank()

    LazyColumn(
        // Tapping outside the search field or scrolling finalizes it.
        modifier.finalizeSearchOn(listState),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Yaku List", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !searching -> "A hand needs at least one yaku to win. A closed hand has no called tiles; " +
                            "calling a tile makes it open, which weakens some yaku and rules others out."
                        else -> resultCount(results.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            !searching -> YakuCategory.entries.forEach { category ->
                item(key = category.name) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            category.blurb,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(results.filter { it.category == category }, key = { it.name }) {
                    YakuCard(it, Modifier.animateItem())
                }
            }

            results.isEmpty() -> item(key = "empty") {
                Text(
                    "No yaku match “${query.trim()}”. Try fewer letters or a different spelling.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp).animateItem(),
                )
            }

            // Ranked by match quality, so category headings would only fragment the list.
            else -> items(results, key = { it.name }) {
                YakuCard(it, Modifier.animateItem(), showCategory = true)
            }
        }
    }
}

@Composable
private fun YakuCard(entry: YakuEntry, modifier: Modifier = Modifier, showCategory: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (showCategory) {
                    Text(
                        entry.category.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    entry.shownMeaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                valueChips(entry.value).forEachIndexed { index, text -> Chip(text, highlighted = index == 0) }
            }
            Text(entry.shownDescription, style = MaterialTheme.typography.bodyMedium)
            val groups = entry.exampleGroups
            if (groups.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    groups.forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            group.forEach { TileFace(it, width = 18.dp) }
                        }
                    }
                }
            }
        }
    }
}

/** The first chip is the headline value; the rest qualify it. */
private fun valueChips(value: YakuValue): List<String> = when (value) {
    YakuValue.Yakuman -> listOf("Yakuman")
    YakuValue.Mangan -> listOf("Mangan")
    is YakuValue.Han -> when {
        value.open == null -> listOf("${value.closed} han", "Closed hand only")
        value.open == value.closed -> listOf("${value.closed} han", "Open or closed")
        else -> listOf("${value.closed} han closed", "${value.open} han open")
    }
}
