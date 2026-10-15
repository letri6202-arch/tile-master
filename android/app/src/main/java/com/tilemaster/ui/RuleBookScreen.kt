package com.tilemaster.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tilemaster.scoring.RuleBook

@Composable
fun RuleBookScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    GuidePage("Riichi Rule Book", RuleBook.intro, RuleBook.sections, onBack, modifier)
}
