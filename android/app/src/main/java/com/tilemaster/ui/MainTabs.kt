package com.tilemaster.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tilemaster.model.Tile
import kotlinx.coroutines.launch

/** The top-level pages, in bottom bar order. To add a page, add an entry and a branch in [MainTabsScreen]. */
enum class MainTab(val label: String, val tile: Tile) {
    DOCS("Learn", Tile.HAKU),
    SCORE("Score", Tile.CHUN),
    COACHING("Coach", Tile.HATSU),
}

@Composable
fun MainTabsScreen(onScoreHand: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(initialPage = MainTab.SCORE.ordinal) { MainTab.entries.size }
    val scope = rememberCoroutineScope()
    val selectTab: (MainTab) -> Unit = { scope.launch { pagerState.animateScrollToPage(it.ordinal) } }

    // Back from another tab returns to scoring; back from scoring leaves the app.
    BackHandler(enabled = pagerState.currentPage != MainTab.SCORE.ordinal) { selectTab(MainTab.SCORE) }

    Column(modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (MainTab.entries[page]) {
                MainTab.DOCS -> DocsScreen()
                MainTab.SCORE -> HomeScreen(onScoreHand = onScoreHand)
                MainTab.COACHING -> CoachingScreen()
            }
        }
        TileNavBar(selected = MainTab.entries[pagerState.targetPage], onSelect = selectTab)
    }
}

@Composable
private fun TileNavBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MainTab.entries.forEach { tab ->
                    TabButton(tab, isSelected = tab == selected, onClick = { onSelect(tab) }, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Fits a 36dp-wide tile face plus its edge, with room for the lift. */
private val SELECTED_TILE_SLOT = 60.dp

@Composable
private fun TabButton(tab: MainTab, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tileWidth by animateDpAsState(if (isSelected) 36.dp else 26.dp, label = "tabTile")
    val backdrop by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
        label = "tabBackdrop",
    )
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(backdrop)
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Sized for the selected tile so the bar keeps its height and the label always fits.
        Box(Modifier.height(SELECTED_TILE_SLOT), contentAlignment = Alignment.BottomCenter) {
            TileFace(tab.tile, width = tileWidth, selected = isSelected)
        }
        Text(
            tab.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
