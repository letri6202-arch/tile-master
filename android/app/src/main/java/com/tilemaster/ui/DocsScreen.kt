package com.tilemaster.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tilemaster.model.Tile

/** The guides that open from the Learn page. */
private enum class DocPage { RULE_BOOK, YAKU_LIST, POINT_GUIDE, GLOSSARY }

@Composable
fun DocsScreen(modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf<DocPage?>(null) }
    BackHandler(enabled = page != null) { page = null }
    // Going deeper slides the new page in from the right; going back reverses it.
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val direction = if (targetState != null) 1 else -1
            (slideInHorizontally(tween(300)) { direction * it / 5 } + fadeIn(tween(300)))
                .togetherWith(slideOutHorizontally(tween(300)) { -direction * it / 5 } + fadeOut(tween(150)))
        },
        modifier = modifier,
        label = "docsPage",
    ) { shown ->
        when (shown) {
            DocPage.RULE_BOOK -> RuleBookScreen(onBack = { page = null })
            DocPage.YAKU_LIST -> YakuListScreen(onBack = { page = null })
            DocPage.GLOSSARY -> GlossaryScreen(onBack = { page = null })
            DocPage.POINT_GUIDE -> PointGuideScreen(onBack = { page = null })
            null -> DocsHub(onOpen = { page = it })
        }
    }
}

@Composable
private fun DocsHub(onOpen: (DocPage) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(top = 24.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Learn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Rules, yaku and scoring, explained",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DocCard(Tile.EAST, "Riichi Rule Book", "How a game of riichi is played", onClick = { onOpen(DocPage.RULE_BOOK) })
        DocCard(Tile.CHUN, "Yaku List", "Learn about each of the yaku (win conditions)", onClick = { onOpen(DocPage.YAKU_LIST) })
        DocCard(Tile.M0, "Point Calculation Guide", "Learn how han, fu and points are calculated", onClick = { onOpen(DocPage.POINT_GUIDE) })
        DocCard(Tile.NORTH, "Glossary", "Quick reminders for the key terms", onClick = { onOpen(DocPage.GLOSSARY) })
    }
}

@Composable
private fun DocCard(tile: Tile, title: String, subtitle: String, onClick: () -> Unit) {
    // The card sinks slightly, the tile lifts and the chevron nudges forward while pressed.
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "cardScale")
    val nudge by animateDpAsState(if (pressed) 4.dp else 0.dp, label = "chevronNudge")
    Surface(
        onClick = onClick,
        interactionSource = interactions,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TileFace(tile, width = 30.dp, selected = pressed)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.offset(x = nudge),
            )
        }
    }
}
