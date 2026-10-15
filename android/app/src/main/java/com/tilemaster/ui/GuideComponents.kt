package com.tilemaster.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The "‹ Learn" button pinned above a guide page. Shrinks slightly and the chevron slides back
 * while pressed.
 */
@Composable
fun GuideBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "backScale")
    val nudge by animateDpAsState(if (pressed) (-4).dp else 0.dp, label = "backNudge")
    FilledTonalButton(
        onClick = onBack,
        interactionSource = interactions,
        modifier = modifier
            .padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Text(
            "‹",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.offset(x = nudge),
        )
        Text(
            "Learn",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** A small pill label. The highlighted one carries the headline value; the others qualify it. */
@Composable
fun Chip(text: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    val container = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * The pinned search field shared by the searchable guides. Results update on every keystroke;
 * Enter just puts the keyboard away and keeps them.
 */
@Composable
fun GuideSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.heightIn(min = 48.dp),
        singleLine = true,
        shape = CircleShape,
        placeholder = { Text(placeholder) },
        // A plain text glyph (U+2315), not an emoji, so it takes the theme colour like the clear button.
        leadingIcon = {
            Text(
                "⌕",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        },
        trailingIcon = if (query.isEmpty()) null else {
            {
                Text(
                    "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Clear search") { onQueryChange("") }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, capitalization = KeyboardCapitalization.None),
        keyboardActions = KeyboardActions(onSearch = {
            focusManager.clearFocus()
            keyboard?.hide()
        }),
    )
}

/**
 * Makes a searchable list finalize its search when the user taps outside the field or scrolls, so
 * the results aren't covered by the keyboard. Returns the modifier to put on the list.
 */
@Composable
fun Modifier.finalizeSearchOn(listState: LazyListState): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) {
                focusManager.clearFocus()
                keyboard?.hide()
            }
        }
    }
    return pointerInput(Unit) { detectTapGestures { focusManager.clearFocus(); keyboard?.hide() } }
}

/** "N results" for the line under a searchable page's title. */
fun resultCount(count: Int): String = if (count == 1) "1 result" else "$count results"
