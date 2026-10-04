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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tilemaster.camera.CameraScreen
import com.tilemaster.model.Tile
import com.tilemaster.ui.HandReviewScreen
import com.tilemaster.ui.TileMasterTheme
import com.tilemaster.ui.TileFace

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

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Screens handle window insets themselves so the camera preview can run edge to edge.
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        if (!hasCameraPermission) {
            PermissionRequired(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
            return@Surface
        }

        when (val screen = vm.screen) {
            Screen.Camera -> CameraScreen(
                detector = vm.detector,
                isProcessing = vm.isProcessing,
                onPhotoCaptured = vm::onPhotoCaptured,
            )

            is Screen.Review -> {
                BackHandler(onBack = vm::retake)
                HandReviewScreen(
                    photo = screen.photo,
                    detections = screen.detections,
                    tiles = vm.tiles,
                    onReplaceTile = vm::replaceTile,
                    onRemoveTile = vm::removeTile,
                    onAddTile = vm::addTile,
                    onRetake = vm::retake,
                )
            }
        }
    }
}

@Composable
private fun PermissionRequired(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A small fanned-out hand as the hero.
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            listOf(Tile.EAST to -10f, Tile.CHUN to 0f, Tile.M0 to 10f).forEach { (tile, angle) ->
                TileFace(
                    tile,
                    width = 56.dp,
                    modifier = Modifier.rotate(angle).offset(y = if (angle == 0f) (-8).dp else 0.dp),
                )
            }
        }
        Text(
            "Tile Master",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            "Point your camera at a hand and it reads the tiles for you. Everything stays on your phone.",
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
    }
}
