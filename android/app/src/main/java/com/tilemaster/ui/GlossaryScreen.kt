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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tilemaster.scoring.GlossaryCatalog
import com.tilemaster.scoring.GlossaryCategory
import com.tilemaster.scoring.GlossarySearch
import com.tilemaster.scoring.GlossaryTerm

@Composable
fun GlossaryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    // Re-ranked on every keystroke, so results appear as you type.
    val results = remember(query) { GlossarySearch.search(GlossaryCatalog.terms, query) }

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        // Pinned above the list so both stay reachable however far the list is scrolled.
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuideBackButton(onBack)
            GuideSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search terms",
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 20.dp, top = 4.dp),
            )
        }
        GlossaryContent(query = query, results = results, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GlossaryContent(query: String, results: List<GlossaryTerm>, modifier: Modifier = Modifier) {
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
                Text("Glossary", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (searching) {
                        resultCount(results.size)
                    } else {
                        "Quick reminders for the words you'll meet while playing. Search by English or Japanese name."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            !searching -> GlossaryCategory.entries.forEach { category ->
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
                items(results.filter { it.category == category }, key = { it.term }) {
                    GlossaryCard(it, Modifier.animateItem())
                }
            }

            results.isEmpty() -> item(key = "empty") {
                Text(
                    "No terms match “${query.trim()}”. Try fewer letters or a different spelling.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp).animateItem(),
                )
            }

            // Ranked by match quality, so category headings would only fragment the list.
            else -> items(results, key = { it.term }) {
                GlossaryCard(it, Modifier.animateItem(), showCategory = true)
            }
        }
    }
}

@Composable
private fun GlossaryCard(term: GlossaryTerm, modifier: Modifier = Modifier, showCategory: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (showCategory) {
                    Text(
                        term.category.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(term.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    term.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(term.description, style = MaterialTheme.typography.bodyMedium)
            val groups = term.exampleGroups
            if (groups.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    groups.forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            group.forEach { TileFace(it, width = 18.dp) }
                        }
                    }
                }
            }
            if (term.aliases.isNotEmpty()) {
                Text(
                    "Also: " + term.aliases.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
