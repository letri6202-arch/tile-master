package com.tilemaster.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tilemaster.model.Tile

@Composable
fun HomeScreen(onScoreHand: () -> Unit, onEnterManually: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TileFan(tileWidth = 52.dp)
            Text(
                "Tile Master",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                "Scan or input your hand to score it!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        ScoreHandCard(onClick = onScoreHand)

        val manualSource = remember { MutableInteractionSource() }
        val manualScale = rememberPressScale(manualSource, enabled = true)
        TextButton(
            onClick = onEnterManually,
            interactionSource = manualSource,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .graphicsLayer {
                    scaleX = manualScale
                    scaleY = manualScale
                },
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Enter tiles manually")
        }
    }
}

/** A small fanned-out hand used as a hero image. */
@Composable
fun TileFan(tileWidth: Dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        listOf(Tile.EAST to -10f, Tile.CHUN to 0f, Tile.M0 to 10f).forEach { (tile, angle) ->
            TileFace(
                tile,
                width = tileWidth,
                modifier = Modifier.rotate(angle).offset(y = if (angle == 0f) (-8).dp else 0.dp),
            )
        }
    }
}

@Composable
private fun ScoreHandCard(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source, enabled = true)
    val pressed by source.collectIsPressedAsState()
    // The arrow leans toward where the card is about to take you.
    val arrowNudge by animateDpAsState(if (pressed) 6.dp else 0.dp, label = "arrowNudge")
    Surface(
        onClick = onClick,
        interactionSource = source,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Scan a Hand",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Scan your tiles, answer a few questions, and get han, fu and points.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(10.dp).offset(x = arrowNudge),
                )
            }
        }
    }
}
