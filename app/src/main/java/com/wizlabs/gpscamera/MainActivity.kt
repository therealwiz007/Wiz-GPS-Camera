package com.wizlabs.gpscamera

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration

private enum class Screen { CAMERA, GALLERY, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) MotionIdle.onTouch()
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        TimeSync.init(applicationContext)
        setContent { ProvideWizMotion { WizApp() } }
    }

    // Volume up/down act as a shutter button while the camera screen is showing.
    private fun isVolume(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val handler = ShutterKeys.handler
        if (isVolume(keyCode) && handler != null) {
            if (event.repeatCount == 0) handler()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isVolume(keyCode) && ShutterKeys.handler != null) return true
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
private fun WizApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repo = remember { SettingsRepo(context) }
    val projects = remember { ProjectStore(context) }
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
    val peek = LocalBackPeek.current
    // Once the back transition has played, drop the peek so the next visit starts full size.
    LaunchedEffect(screen) { kotlinx.coroutines.delay(650); if (peek != null && !peek.dragging) peek.reset() }

    WizTheme {
        if (!repo.settings.onboardingDone) {
            Onboarding(onDone = { repo.update { it.copy(onboardingDone = true) } })
        } else {
            // The app stays closed until precise location is allowed, GPS is on and a fresh fix exists.
            GpsGate(location) {
                // Camera is "home": other screens slide in from the right and back out to it.
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val forward = targetState != Screen.CAMERA
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(tween(320, easing = WizMotion.Emphasized)) { dir * it / 5 } + fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(260)) { -dir * it / 8 } + fadeOut(tween(180)))
                    },
                    label = "screen"
                ) { current ->
                    when (current) {
                        Screen.CAMERA -> CameraScreen(
                            repo = repo,
                            projects = projects,
                            location = location,
                            sensors = sensors,
                            onGallery = { screen = Screen.GALLERY },
                            onSettings = { screen = Screen.SETTINGS }
                        )
                        Screen.GALLERY -> GalleryScreen(repo = repo, projects = projects, onBack = { screen = Screen.CAMERA })
                        Screen.SETTINGS -> Box(Modifier.fillMaxSize().backPeek("SETTINGS")) {
                            SettingsScreen(
                                repo = repo,
                                projects = projects,
                                location = location,
                                sensors = sensors,
                                onBack = { screen = Screen.CAMERA }
                            )
                        }
                    }
                }
            }
        }
    }
}
