package com.wizlabs.gpscamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration

private enum class Screen { CAMERA, GALLERY, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        TimeSync.init(applicationContext)
        setContent { WizApp() }
    }
}

@Composable
private fun WizApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repo = remember { SettingsRepo(context) }
    val location = remember { LocationEngine(context) }
    val sensors = remember { SensorHub(context) }
    var screen by remember { mutableStateOf(Screen.CAMERA) }

    // GPS and sensors only run while the app is visible.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    sensors.start()
                    location.start()
                }
                Lifecycle.Event.ON_STOP -> {
                    sensors.stop()
                    location.stop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensors.stop()
            location.stop()
        }
    }

    BackHandler(enabled = screen != Screen.CAMERA) { screen = Screen.CAMERA }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = WizTeal,
            onPrimary = WizNavy,
            background = WizNavy,
            surface = WizPanel,
            onSurface = Color.White
        )
    ) {
        when (screen) {
            Screen.CAMERA -> CameraScreen(
                repo = repo,
                location = location,
                sensors = sensors,
                onGallery = { screen = Screen.GALLERY },
                onSettings = { screen = Screen.SETTINGS }
            )
            Screen.GALLERY -> GalleryScreen(onBack = { screen = Screen.CAMERA })
            Screen.SETTINGS -> SettingsScreen(repo = repo, onBack = { screen = Screen.CAMERA })
        }
    }
}
