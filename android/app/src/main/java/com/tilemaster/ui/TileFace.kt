package com.tilemaster.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tilemaster.model.Suit
import com.tilemaster.model.Tile

private val FaceTop = Color(0xFFFFFCF3)
private val FaceBottom = Color(0xFFEFE6D0)
private val FaceEdge = Color(0xFFD8CBAE)
private val TileBack = Color(0xFF2E8B57)
private val TileBackShade = Color(0xFF1E6B41)
private val TileRed = Color(0xFFC62828)
private val TileGreen = Color(0xFF2E7D32)
private val TileBlue = Color(0xFF1565C0)
private val TileInk = Color(0xFF1F1B16)

private fun Tile.inkColor(): Color = when {
    isRedFive || this == Tile.CHUN -> TileRed
    this == Tile.HATSU -> TileGreen
    suit == Suit.SOU -> TileGreen
    suit == Suit.PIN -> TileBlue
    else -> TileInk
}

/** Height of a tile face relative to its width (real Riichi tiles are about 3:4). */
private const val ASPECT = 1.36f

/**
 * A tile drawn like a physical one: an ivory face on top of a green back, so it reads as having
 * thickness. Text scales with [width], not with the system font size, so it always fits the face.
 */
@Composable
fun TileFace(
    tile: Tile,
    modifier: Modifier = Modifier,
    width: Dp = 40.dp,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val faceHeight = width * ASPECT
    val depth = width * 0.12f
    val shape = RoundedCornerShape(width * 0.16f)
    val lift by animateDpAsState(if (selected) depth * 1.5f else 0.dp, label = "tileLift")
    val density = LocalDensity.current
    val topSize = with(density) { (width * if (tile.suit == Suit.HONOR) 0.58f else 0.52f).toSp() }
    val bottomSize = with(density) { (width * 0.3f).toSp() }

    Box(
        modifier
            .size(width, faceHeight + depth)
            .offset(y = -lift)
            .semantics { contentDescription = tile.displayName },
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .size(width, faceHeight)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(TileBack, TileBackShade))),
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .size(width, faceHeight)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(FaceTop, FaceBottom)))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) TableColors.Gold else FaceEdge,
                    shape = shape,
                )
                .let { if (onClick != null) it.clickable(onClick = onClick) else it },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(tile.faceTop, color = tile.inkColor(), fontSize = topSize, fontWeight = FontWeight.Bold, lineHeight = topSize)
            tile.faceBottom?.let { Text(it, color = tile.inkColor(), fontSize = bottomSize, lineHeight = bottomSize) }
        }
        if (tile.isRedFive) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(width * 0.1f)
                    .size(width * 0.14f)
                    .clip(CircleShape)
                    .background(TileRed),
            )
        }
    }
}

/** An empty dashed tile slot with a plus, used to add a missed tile. */
@Composable
fun AddTileSlot(onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp = 40.dp) {
    val faceHeight = width * ASPECT
    val depth = width * 0.12f
    val shape = RoundedCornerShape(width * 0.16f)
    val color = TableColors.Gold.copy(alpha = 0.8f)
    val plusSize = with(LocalDensity.current) { (width * 0.55f).toSp() }
    Box(
        modifier
            .size(width, faceHeight + depth)
            .semantics { contentDescription = "Add a tile" },
    ) {
        Box(
            Modifier
                .size(width, faceHeight)
                .clip(shape)
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = size.copy(width = size.width - stroke, height = size.height - stroke),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius((width * 0.16f).toPx()),
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                    )
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = color, fontSize = plusSize, fontWeight = FontWeight.Light)
        }
    }
}
