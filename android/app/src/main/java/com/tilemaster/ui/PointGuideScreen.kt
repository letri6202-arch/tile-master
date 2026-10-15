package com.tilemaster.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tilemaster.scoring.PointGuide

@Composable
fun PointGuideScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    GuidePage("Point Calculation", PointGuide.intro, PointGuide.sections, onBack, modifier)
}
