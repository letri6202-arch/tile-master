package com.tilemaster

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tilemaster.camera.CameraScreen
import com.tilemaster.ui.HandReviewScreen
import com.tilemaster.ui.MainTabsScreen
import com.tilemaster.ui.ManualEntryScreen
import com.tilemaster.ui.QuestionsScreen
import com.tilemaster.ui.ResultScreen
import com.tilemaster.ui.TileMasterTheme
import com.tilemaster.ui.TileFan

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The UI is always dark, so keep system bar icons light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            TileMasterTheme {
                App()
            }
        }
    }
}

@Composable
private fun App(vm: ScannerViewModel = viewModel()) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    // Ask for the camera only once the player heads to it, not on the home screen.
    val needsCamera = vm.screen is Screen.Camera
    LaunchedEffect(needsCamera) {
        if (needsCamera && !hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Screens handle window insets themselves so the camera preview can run edge to edge.
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = vm.screen,
            contentKey = { it::class },
            transitionSpec = { screenTransition(initialState, targetState) },
            label = "screen",
        ) { screen ->
        when (screen) {
            Screen.Home -> MainTabsScreen(onScoreHand = vm::startScoring, onEnterManually = vm::startManualEntry)

            is Screen.Camera -> {
                val forIndicators = screen.target != CaptureTarget.HAND
                val closeCamera = if (forIndicators) vm::closeIndicatorCamera else vm::goHome
                BackHandler(onBack = closeCamera)
                if (!hasCameraPermission) {
                    PermissionRequired(
                        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onBack = closeCamera,
                    )
                    return@AnimatedContent
                }
                CameraScreen(
                    detector = vm.detector,
                    isProcessing = vm.isProcessing,
                    onPhotoCaptured = vm::onPhotoCaptured,
                    title = when (screen.target) {
                        CaptureTarget.HAND -> "Tile Master"
                        CaptureTarget.DORA -> "Dora indicators"
                        CaptureTarget.URA_DORA -> "Ura dora indicators"
                    },
                    idleHint = if (forIndicators) "Point at the indicator tiles" else "Lay your hand out in one row",
                    onClose = closeCamera,
                )
            }

            Screen.Review -> {
                BackHandler(onBack = vm::retake)
                val scan = vm.scan
                if (scan != null) {
                    HandReviewScreen(
                        photo = scan.photo,
                        detections = scan.detections,
                        tiles = vm.tiles,
                        onReplaceTile = vm::replaceTile,
                        onRemoveTile = vm::removeTile,
                        onAddTile = vm::addTile,
                        onRetake = vm::retake,
                        onNext = vm::startQuestions,
                    )
                }
            }

            Screen.ManualEntry -> {
                BackHandler(onBack = vm::goHome)
                ManualEntryScreen(
                    tiles = vm.tiles,
                    onAddTile = vm::addTile,
                    onRemoveTile = vm::removeTile,
                    onClear = vm::clearTiles,
                    onBack = vm::goHome,
                    onNext = vm::startQuestions,
                )
            }

            is Screen.Questions -> {
                BackHandler(onBack = vm::previousStep)
                QuestionsScreen(
                    step = screen.step,
                    tiles = vm.tiles,
                    context = vm.handContext,
                    isProcessing = vm.isProcessing,
                    onUpdate = vm::updateContext,
                    onAddMeld = vm::addMeld,
                    onRemoveMeld = vm::removeMeld,
                    onScanIndicators = vm::scanIndicators,
                    onGoToStep = vm::goToStep,
                    onBack = vm::previousStep,
                    onNext = vm::nextStep,
                )
            }

            is Screen.Result -> {
                BackHandler(onBack = vm::editAnswers)
                ResultScreen(
                    outcome = screen.outcome,
                    onEditAnswers = vm::editAnswers,
                    onScanNext = vm::retake,
                    onHome = vm::goHome,
                )
            }
        }
        }
    }
}

/** Where a screen sits in the scoring journey; moving to a higher rank slides forward. */
private fun Screen.rank(): Int = when (this) {
    Screen.Home -> 0
    Screen.ManualEntry -> 1
    is Screen.Camera -> if (target == CaptureTarget.HAND) 1 else 4
    Screen.Review -> 2
    is Screen.Questions -> 3
    is Screen.Result -> 5
}

/** Forward moves slide in from the right with a slight grow; backward moves mirror it. */
private fun screenTransition(from: Screen, to: Screen): ContentTransform {
    val forward = to.rank() >= from.rank()
    val direction = if (forward) 1 else -1
    val slide = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 320)
    return (
        slideInHorizontally(slide) { direction * it / 6 } +
            fadeIn(tween(260, delayMillis = 40)) +
            scaleIn(tween(320), initialScale = if (forward) 0.96f else 1.04f)
        ) togetherWith (
        slideOutHorizontally(slide) { -direction * it / 6 } +
            fadeOut(tween(160)) +
            scaleOut(tween(320), targetScale = if (forward) 1.04f else 0.96f)
        )
}

@Composable
private fun PermissionRequired(onRequest: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TileFan(tileWidth = 56.dp)
        Text(
            "Allow the camera",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            "Tile Master reads your tiles from a photo. Everything stays on your phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(
            onClick = onRequest,
            modifier = Modifier.padding(top = 32.dp).fillMaxWidth().height(52.dp),
        ) {
            Text("Allow camera access")
        }
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Text("Not now")
        }
    }
}
