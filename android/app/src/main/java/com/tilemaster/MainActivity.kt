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
        when (val screen = vm.screen) {
            Screen.Home -> MainTabsScreen(onScoreHand = vm::startScoring)

            is Screen.Camera -> {
                val forIndicators = screen.target != CaptureTarget.HAND
                val closeCamera = if (forIndicators) vm::closeIndicatorCamera else vm::goHome
                BackHandler(onBack = closeCamera)
                if (!hasCameraPermission) {
                    PermissionRequired(
                        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onBack = closeCamera,
                    )
                    return@Surface
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
