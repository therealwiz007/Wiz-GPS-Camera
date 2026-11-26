@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.wizlabs.gpscamera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import android.media.MediaActionSound
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** Lets the activity route the volume keys to the shutter while the camera is on screen. */
object ShutterKeys {
    @Volatile
    var handler: (() -> Unit)? = null
}

private val CAMERA_PERMS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/** Draw the offscreen map view into a bitmap (reusing [reuse] when the size matches). */
private fun snapshotMap(view: MapView?, reuse: Bitmap?): Bitmap? {
    if (view == null || view.width <= 0 || view.height <= 0) return null
    val bmp = if (reuse != null && !reuse.isRecycled && reuse.width == view.width && reuse.height == view.height) reuse
    else Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(0xFF233D49.toInt())
    view.draw(android.graphics.Canvas(bmp))
    return bmp
}

/** Thumbnail of the newest photo this app saved, or null when there is none. */
private fun latestThumb(context: Context): Bitmap? = runCatching {
    val item = loadPhotos(context).firstOrNull() ?: return null
    context.contentResolver.loadThumbnail(item.uri, Size(240, 240), null)
}.getOrNull()

@Composable
fun CameraScreen(
    repo: SettingsRepo,
    projects: ProjectStore,
    location: LocationEngine,
    sensors: SensorHub,
    onGallery: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val settings = repo.settings
    val tickHaptic = rememberTick()

    var hasCamera by remember { mutableStateOf(isGranted(context, Manifest.permission.CAMERA)) }
    var hasLocation by remember {
        mutableStateOf(
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var showExposure by remember { mutableStateOf(false) }
    var exposureIndex by remember { mutableIntStateOf(0) }
    var showProjectPicker by remember { mutableStateOf(false) }
    val activeProject = projects.byId(settings.projectId)
    val sound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var status by remember { mutableStateOf("") }
    var lastStatus by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var zoomText by remember { mutableStateOf<String?>(null) }
    var zoomStamp by remember { mutableLongStateOf(0L) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showStampSheet by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<Bitmap?>(null) }
    var lastThumb by remember { mutableStateOf<Bitmap?>(null) }
    var thumbVersion by remember { mutableIntStateOf(0) }
    val thumbScale = remember { Animatable(1f) }
    // Motion: decorative art runs on a ≤30 fps clock that stops while capturing or unfocused.
    val motion = rememberMotionAllowed()
    // Only the "searching" GPS glyph reads this clock, so it stops once a fresh fix is locked.
    val gpsLocked = location.fix.let { f -> hasLocation && f != null && !f.isMock && f.ageSeconds() <= 10 }
    val decor = rememberThrottledClock(running = motion && !busy && !gpsLocked, fps = 15, settle = false)
    var captureKey by remember { mutableIntStateOf(0) }
    val flash = remember { Animatable(0f) }
    val fly = remember { Animatable(1f) }
    var flyBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var thumbBounds by remember { mutableStateOf(Rect.Zero) }
    var previewBounds by remember { mutableStateOf(Rect.Zero) }
    val stampIn = remember { Animatable(1f) }
    val lastLook = remember { arrayOfNulls<Any>(1) }
    val providerHolder = remember { arrayOfNulls<ProcessCameraProvider>(1) }
    val logo = remember(settings.logoVersion) { LogoStore.load(context) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasCamera = isGranted(context, Manifest.permission.CAMERA)
        hasLocation = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (hasLocation) location.start()
    }

    LaunchedEffect(Unit) {
        if (!hasCamera || !hasLocation) permLauncher.launch(CAMERA_PERMS)
    }

    // Once-a-second tick: refreshes the "GPS fix age" chip.
    val tick by produceState(0L) {
        while (true) {
            value = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }

    // Keep the network time anchor fresh while the camera is open.
    LaunchedEffect(Unit) {
        while (true) {
            TimeSync.sync(context)
            delay(5 * 60_000L)
        }
    }

    LaunchedEffect(settings.showWeather) { location.weatherEnabled = settings.showWeather }

    LaunchedEffect(status) {
        if (status.isNotEmpty()) {
            lastStatus = status
            delay(3000)
            status = ""
        }
    }

    LaunchedEffect(zoomStamp) {
        delay(1100)
        zoomText = null
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1200)
            focusPoint = null
        }
    }

    // Last-photo thumbnail: loaded on open and after every save, with a little "pop".
    LaunchedEffect(thumbVersion) {
        val loaded = withContext(Dispatchers.IO) { latestThumb(context) }
        // New photo: it flies from the viewfinder into the gallery slot, then the slot pops.
        if (thumbVersion > 0 && loaded != null && motion && !previewBounds.isEmpty && !thumbBounds.isEmpty) {
            flyBitmap = loaded
            fly.snapTo(0f)
            fly.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            flyBitmap = null
        }
        lastThumb = loaded
        if (thumbVersion > 0) {
            thumbScale.snapTo(0.55f)
            thumbScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    // ---- Bind CameraX -----------------------------------------------------------------------
    LaunchedEffect(previewView, lens, settings.megapixels, settings.aspect, hasCamera) {
        val pv = previewView ?: return@LaunchedEffect
        if (!hasCamera) return@LaunchedEffect
        try {
            val provider = suspendCancellableCoroutine<ProcessCameraProvider> { c ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    try {
                        c.resume(future.get())
                    } catch (e: Exception) {
                        c.resumeWithException(e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
            providerHolder[0] = provider
            // 16:9 uses the wide sensor mode; 4:3 and 1:1 both use 4:3 (1:1 is cropped when saving).
            val strategy = if (settings.aspect == AspectChoice.R16_9) {
                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            } else {
                AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            }
            val previewSelector = ResolutionSelector.Builder().setAspectRatioStrategy(strategy).build()
            val preview = Preview.Builder()
                .setResolutionSelector(previewSelector)
                .build()
                .also { it.setSurfaceProvider(pv.surfaceProvider) }
            // Cap the still photo at the chosen megapixel count: keep the largest supported
            // output at or below the cap (falling back to everything if none qualify).
            val capPixels = settings.megapixels * 1_000_000L
            val selector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(strategy)
                .setResolutionFilter { supportedSizes, _ ->
                    val within = supportedSizes.filter { it.width.toLong() * it.height.toLong() <= capPixels }
                    (if (within.isNotEmpty()) within else supportedSizes)
                        .sortedByDescending { it.width.toLong() * it.height.toLong() }
                }
                .build()
            val ic = ImageCapture.Builder()
                .setResolutionSelector(selector)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(100)
                .build()
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.Builder().requireLensFacing(lens).build(),
                preview,
                ic
            )
            zoomRatio = 1f
            exposureIndex = 0
            imageCapture = ic
        } catch (e: kotlinx.coroutines.CancellationException) {
            // The screen was left or the camera settings changed; not an error.
            throw e
        } catch (e: Exception) {
            status = "Camera unavailable: ${e.localizedMessage}"
        }
    }

    DisposableEffect(Unit) {
        onDispose { providerHolder[0]?.unbindAll() }
    }

    LaunchedEffect(imageCapture, camera, settings.flash, lens) {
        val back = lens == CameraSelector.LENS_FACING_BACK
        imageCapture?.flashMode = when {
            !back -> ImageCapture.FLASH_MODE_OFF
            settings.flash == FlashChoice.AUTO -> ImageCapture.FLASH_MODE_AUTO
            settings.flash == FlashChoice.ON -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) runCatching { cam.cameraControl.enableTorch(back && settings.flash == FlashChoice.TORCH) }
        }
    }

    // Keep the photo upright (and the stamp on the right edge) even when the phone is rotated.
    val captureRef = rememberUpdatedState(imageCapture)
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                captureRef.value?.targetRotation = rotation
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }

    DisposableEffect(mapView) {
        val view = mapView
        onDispose {
            view?.onPause()
            view?.onDetach()
        }
    }

    fun setZoom(target: Float) {
        val cam = camera ?: return
        val zs = cam.cameraInfo.zoomState.value ?: return
        val z = target.coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
        cam.cameraControl.setZoomRatio(z)
        zoomRatio = z
    }

    // ---- Shutter ----------------------------------------------------------------------------
    val shoot: () -> Unit = {
        scope.launch {
            if (busy) return@launch
            val ic = imageCapture
            if (ic == null) {
                status = "Camera is not ready"
                return@launch
            }
            busy = true
            try {
                val s = repo.settings
                if (s.timerSeconds > 0) {
                    for (sec in s.timerSeconds downTo 1) {
                        countdown = sec
                        tickHaptic()
                        delay(1000)
                    }
                    countdown = 0
                }
                var saved = 0
                for (i in 0 until s.burstCount) {
                    val fix = location.fix
                    if (fix?.isMock == true && s.mockPolicy == MockPolicy.BLOCK) {
                        status = "Blocked: mock location detected"
                        break
                    }
                    val project = projects.byId(s.projectId)
                    val data = buildStampData(
                        s, fix, location.address, location.addressCached, location.weather,
                        sensors.headingDeg, location.declination, newPhotoId(), project?.name ?: ""
                    )
                    val mapCopy = snapshotMap(mapView, null)
                    if (s.vibrate) Haptics.shutter(context)
                    if (s.shutterSound) sound.play(MediaActionSound.SHUTTER_CLICK)
                    captureKey++
                    launch {
                        flash.snapTo(.85f)
                        flash.animateTo(0f, tween(if (motion) 280 else 60))
                    }
                    val res = suspendCancellableCoroutine<Result<PhotoPipeline.Saved>> { c ->
                        val front = lens == CameraSelector.LENS_FACING_FRONT
                        val reverse = if (front && !s.mirrorFront) false else null
                        PhotoPipeline.capture(context, ic, data, s, mapCopy, logo, project, reverse) { c.resume(it) }
                    }
                    if (res.isSuccess) {
                        saved++
                        thumbVersion++
                    } else {
                        status = "Capture failed: ${res.exceptionOrNull()?.localizedMessage}"
                        break
                    }
                    if (i < s.burstCount - 1) delay(300)
                }
                if (saved == 1) status = "Saved to gallery" else if (saved > 1) status = "$saved photos saved"
            } finally {
                busy = false
                countdown = 0
            }
        }
        Unit
    }

    // Volume keys take a photo while this screen is showing (if enabled in Settings).
    val shootRef = rememberUpdatedState(shoot)
    DisposableEffect(settings.volumeShutter) {
        ShutterKeys.handler = if (settings.volumeShutter) ({ shootRef.value() }) else null
        onDispose { ShutterKeys.handler = null }
    }

    // ---- UI ---------------------------------------------------------------------------------
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Offscreen map source: it sits behind everything but still renders, so we can snapshot
        // it for the live stamp and for each photo. (The red pin is drawn by the stamp itself.)
        AndroidView(
            modifier = Modifier.size(200.dp),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(false)
                    controller.setZoom(17.0)
                    setUseDataConnection(true)
                    mapView = this
                }
            },
            update = { map ->
                val fix = location.fix
                if (fix != null) {
                    val point = GeoPoint(fix.lat, fix.lon)
                    // Post so zoom and centre are applied after the view has been laid out.
                    map.post {
                        map.controller.setZoom(17.0)
                        map.controller.setCenter(point)
                        map.invalidate()
                    }
                }
            }
        )
        Box(Modifier.fillMaxSize().background(Color.Black))

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val portrait = maxHeight >= maxWidth
            val ratio = if (portrait) 1f / settings.aspect.ratio else settings.aspect.ratio
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            // ---- Preview frame -----------------------------------------------------------------
            Box(
                Modifier
                    .align(if (portrait) Alignment.TopCenter else Alignment.Center)
                    .padding(top = if (portrait) topInset + 62.dp else 0.dp)
                    .then(if (portrait) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
                    .aspectRatio(ratio, matchHeightConstraintsFirst = !portrait)
                    .onGloballyPositioned { previewBounds = it.boundsInRoot() }
                    .clip(RoundedCornerShape(if (portrait) 22.dp else 0.dp))
                    .clipToBounds()
                    .background(Color(0xFF060B18))
            ) {
                if (hasCamera) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                previewView = this
                            }
                        }
                    )
                } else {
                    Column(
                        Modifier.align(Alignment.Center).padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Camera access needed", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Wiz GPS Camera needs the camera to take geo-tagged photos.",
                            color = GpsMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { permLauncher.launch(CAMERA_PERMS) },
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Allow camera") }
                    }
                }

                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
                    val hPx = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
                    val layerW = min(wPx, 720)
                    val layerH = (layerW.toLong() * hPx / wPx).toInt().coerceAtLeast(1)

                    // Live stamp: rendered with the same code as the saved photo.
                    LaunchedEffect(settings, layerW, layerH, logo) {
                        val buffers = arrayOf(
                            Bitmap.createBitmap(layerW, layerH, Bitmap.Config.ARGB_8888),
                            Bitmap.createBitmap(layerW, layerH, Bitmap.Config.ARGB_8888)
                        )
                        var index = 0
                        var mapBuffer: Bitmap? = null
                        while (true) {
                            val data = buildStampData(
                                settings, location.fix, location.address, location.addressCached,
                                location.weather, sensors.headingDeg, location.declination, "WGC-LIVE",
                                projects.byId(settings.projectId)?.name ?: ""
                            )
                            mapBuffer = snapshotMap(mapView, mapBuffer)
                            val target = buffers[index]
                            index = 1 - index
                            val mapForDraw = mapBuffer
                            withContext(Dispatchers.Default) {
                                target.eraseColor(0)
                                StampRenderer.draw(android.graphics.Canvas(target), target.width, target.height, data, settings, mapForDraw, logo)
                            }
                            overlay = target
                            // Slide the stamp in when its look changes (not on every settings edit).
                            val look = listOf(settings.template, settings.position, settings.style, logo)
                            if (lastLook[0] != look) {
                                val first = lastLook[0] == null
                                lastLook[0] = look
                                if (motion && !first) {
                                    stampIn.snapTo(0f)
                                    launch { stampIn.animateTo(1f, tween(420, easing = WizMotion.Emphasized)) }
                                }
                            }
                            delay(1000)
                        }
                    }

                    overlay?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                val v = stampIn.value
                                alpha = v
                                val dir = when (settings.position) {
                                    StampPosition.TOP -> -1f
                                    else -> 1f
                                }
                                if (settings.position == StampPosition.LEFT) translationX = -(1f - v) * 48.dp.toPx()
                                else translationY = dir * (1f - v) * 48.dp.toPx()
                            }
                        )
                    }
                }

                if (settings.gridOn) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Color.White.copy(alpha = .30f)
                        for (i in 1..2) {
                            val x = size.width * i / 3f
                            val y = size.height * i / 3f
                            drawLine(c, Offset(x, 0f), Offset(x, size.height), 1.2f)
                            drawLine(c, Offset(0f, y), Offset(size.width, y), 1.2f)
                        }
                    }
                }

                if (settings.levelOn && sensors.levelValid) {
                    Canvas(Modifier.fillMaxSize()) {
                        val dev = sensors.levelDeg
                        val level = abs(dev) < 1f
                        val color = if (level) GpsGood else Color.White.copy(alpha = .85f)
                        val cy = size.height / 2f
                        drawLine(color.copy(alpha = .5f), Offset(size.width * .40f, cy), Offset(size.width * .45f, cy), 3f)
                        drawLine(color.copy(alpha = .5f), Offset(size.width * .55f, cy), Offset(size.width * .60f, cy), 3f)
                        rotate(dev) {
                            drawLine(color, Offset(size.width * .28f, cy), Offset(size.width * .72f, cy), 4f, cap = StrokeCap.Round)
                        }
                    }
                }

                // Gesture layer: tap to focus, pinch to zoom.
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(camera, previewView) {
                            detectTapGestures { offset ->
                                val cam = camera
                                val pv = previewView
                                if (cam != null && pv != null) {
                                    focusPoint = offset
                                    runCatching {
                                        val point = pv.meteringPointFactory.createPoint(offset.x, offset.y)
                                        val action = FocusMeteringAction.Builder(
                                            point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                        cam.cameraControl.startFocusAndMetering(action)
                                    }
                                }
                            }
                        }
                        .pointerInput(camera) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val cam = camera ?: return@detectTransformGestures
                                if (zoom == 1f) return@detectTransformGestures
                                val zs = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                                val target = (zs.zoomRatio * zoom).coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
                                cam.cameraControl.setZoomRatio(target)
                                zoomRatio = target
                                zoomText = zoomLabel(target)
                                zoomStamp = System.nanoTime()
                            }
                        }
                )

                focusPoint?.let { p -> FocusRing(p) }

                // Quick row over the top of the preview: flash, timer, ratio, GPS chip, grid, exposure.
                val quickBack = lens == CameraSelector.LENS_FACING_BACK
                CameraQuickRow(
                    settings = settings,
                    backLens = quickBack,
                    exposureActive = showExposure || exposureIndex != 0,
                    onFlash = {
                        val next = settings.flash.next()
                        repo.update { it.copy(flash = next) }
                        status = "Flash: ${next.label}"
                    },
                    onTimer = {
                        val next = when (settings.timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 }
                        repo.update { it.copy(timerSeconds = next) }
                        status = if (next == 0) "Timer off" else "Timer ${next}s"
                    },
                    onAspect = { repo.update { it.copy(aspect = it.aspect.next()) } },
                    onGrid = { repo.update { it.copy(gridOn = !it.gridOn) } },
                    onExposure = { showExposure = !showExposure },
                    gpsChip = {
                        GpsChip(
                            fix = location.fix,
                            hasLocation = hasLocation,
                            address = location.address,
                            tick = tick,
                            clock = decor,
                            onClick = {
                                if (hasLocation) {
                                    val f = location.fix
                                    status = if (f == null) "Searching GPS…" else
                                        "GPS " + (f.accuracyM?.let { "±${it.toInt()} m · " } ?: "") + "updated ${f.ageSeconds()}s ago"
                                    location.start()
                                    location.refresh()
                                } else {
                                    permLauncher.launch(CAMERA_PERMS)
                                }
                            }
                        )
                    },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).enterRise(1)
                )

                // Exposure slider, near the top so it never covers the stamp.
                val cam = camera
                if (showExposure && cam != null && cam.cameraInfo.exposureState.isExposureCompensationSupported) {
                    val es = cam.cameraInfo.exposureState
                    val range = es.exposureCompensationRange
                    val step = es.exposureCompensationStep.toFloat()
                    Row(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 62.dp, start = 20.dp, end = 20.dp)
                            .background(GpsGlass, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            String.format(Locale.US, "%+.1f EV", exposureIndex * step),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(60.dp)
                        )
                        Slider(
                            value = exposureIndex.toFloat(),
                            onValueChange = {
                                val idx = it.roundToInt().coerceIn(range.lower, range.upper)
                                if (idx != exposureIndex) {
                                    exposureIndex = idx
                                    cam.cameraControl.setExposureCompensationIndex(idx)
                                }
                            },
                            valueRange = range.lower.toFloat()..range.upper.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = GpsTeal, activeTrackColor = GpsTeal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Live compass, top-right under the GPS chip.
                val heading = sensors.headingDeg
                if (settings.showCompass && heading != null) {
                    val trueHeading = ((heading + (location.declination ?: 0f)) % 360f + 360f) % 360f
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 112.dp, end = 12.dp)
                            .enterRise(2)
                            .background(GpsGlass, RoundedCornerShape(50))
                            .padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompassRose(heading = trueHeading)
                        Spacer(Modifier.width(6.dp))
                        Text(StampFormat.heading(trueHeading), color = Color.White, style = MaterialTheme.typography.labelSmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }

                AnimatedVisibility(
                    visible = status.isNotEmpty(),
                    enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { -it / 2 },
                    exit = fadeOut(tween(220)),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 112.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        lastStatus,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .background(Color(0xD9060B18), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }

                zoomText?.let {
                    Text(
                        it,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color(0x99060B18), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (countdown > 0) {
                    CountdownBadge(countdown, Modifier.align(Alignment.Center))
                }

                ViewfinderCorners(appearKey = Triple(lens, settings.aspect, hasCamera))

                // Capture flash: fades out in the draw phase, nothing drawn when idle.
                Spacer(
                    Modifier.fillMaxSize().drawBehind {
                        val a = flash.value
                        if (a > 0.01f) drawRect(Color.White.copy(alpha = a))
                    }
                )
            }

            // ---- Top glass bar + bottom controls (camera/TopBar.kt, camera/ShutterBar.kt) --------
            val backLens = lens == CameraSelector.LENS_FACING_BACK
            CameraTopBar(
                projectName = activeProject?.name,
                levelOn = settings.levelOn,
                noteSet = settings.note.isNotBlank(),
                templateLabel = settings.template.label,
                onProject = { showProjectPicker = true },
                onLevel = { repo.update { it.copy(levelOn = !it.levelOn) } },
                onNote = { showNoteDialog = true },
                onStamp = { showStampSheet = true },
                onSettings = onSettings,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 3.dp)
                    .enterRise(0, rise = -18f)
            )

            CameraShutterBar(
                camera = camera,
                zoomRatio = zoomRatio,
                onZoom = { setZoom(it) },
                thumb = remember(lastThumb) { lastThumb?.asImageBitmap() },
                thumbScale = thumbScale,
                onThumbBounds = { thumbBounds = it },
                onGallery = onGallery,
                shutterReady = !busy && imageCapture != null,
                shutterBusy = busy && countdown == 0,
                burst = settings.burstCount,
                captureKey = captureKey,
                onShoot = shoot,
                backLens = backLens,
                onFlip = {
                    lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                    else CameraSelector.LENS_FACING_BACK
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // The new photo flies from the viewfinder into the gallery slot along a small arc.
        flyBitmap?.let { bmp ->
            val tb = thumbBounds
            val pb = previewBounds
            val shape = RoundedCornerShape(16.dp)
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset { IntOffset(tb.left.roundToInt(), tb.top.roundToInt()) }
                    .size(with(density) { tb.width.toDp() }, with(density) { tb.height.toDp() })
                    .graphicsLayer {
                        val e = fly.value
                        val arc = kotlin.math.sin(e * Math.PI).toFloat()
                        val startW = pb.width * 0.6f
                        val s0 = startW / tb.width
                        val sc = s0 + (1f - s0) * e
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = sc
                        scaleY = sc
                        translationX = (pb.center.x - startW / 2f - tb.left) * (1f - e)
                        translationY = (pb.center.y - startW / 2f - tb.top) * (1f - e) - arc * 56.dp.toPx()
                        rotationZ = -9f * arc
                        this.shape = shape
                        clip = true
                    }
                    .border(1.5.dp, Color.White, shape)
            )
        }
    }

    if (showStampSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStampSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GpsSurface,
            contentColor = GpsSilver,
            dragHandle = { BottomSheetDefaults.DragHandle(color = GpsMuted) }
        ) {
            StampSheet(
                settings = settings,
                repo = repo,
                onMore = {
                    showStampSheet = false
                    onSettings()
                }
            )
        }
    }

    if (showNoteDialog) {
        var draft by remember { mutableStateOf(settings.note) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            containerColor = GpsSurface,
            title = { Text("Note on photo") },
            text = {
                Column {
                    Text(
                        "Printed on every photo until you change it.",
                        color = GpsMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        maxLines = 3,
                        placeholder = { Text("e.g. Inspection – Pump 2") },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GpsTeal,
                            unfocusedBorderColor = GpsLine,
                            cursorColor = GpsTeal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (settings.tags.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("Quick tags", color = GpsMuted, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            settings.tags.forEach { tag ->
                                PillChip(tag, false) { draft = if (draft.isBlank()) tag else "${draft.trim()} · $tag" }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repo.update { it.copy(note = draft.trim()) }
                        showNoteDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (settings.note.isNotBlank()) {
                        TextButton(onClick = {
                            repo.update { it.copy(note = "") }
                            showNoteDialog = false
                        }) { Text("Clear", color = GpsBad) }
                    }
                    TextButton(onClick = { showNoteDialog = false }) { Text("Cancel", color = GpsMuted) }
                }
            }
        )
    }

    if (showProjectPicker) {
        ProjectPickerDialog(
            projects = projects,
            selectedId = settings.projectId,
            onSelect = { id ->
                repo.update { it.copy(projectId = id) }
                showProjectPicker = false
            },
            onDismiss = { showProjectPicker = false }
        )
    }
}

/** Choose the project new photos are filed under, or create one. */
