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

/** Colour that summarises GPS health: teal = fresh, amber = ageing / searching, red = off / stale / mock. */
private fun gpsColor(fix: GpsFix?, hasLocation: Boolean): Color = when {
    !hasLocation -> WizBad
    fix == null -> WizWarn
    fix.isMock -> WizBad
    fix.ageSeconds() <= 10 -> WizTeal
    fix.ageSeconds() <= 60 -> WizWarn
    else -> WizBad
}

private fun zoomLabel(z: Float): String =
    if (z < 0.95f || abs(z - z.toInt()) > 0.05f) String.format(Locale.US, "%.1f×", z)
    else "${z.toInt()}×"

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
    var blink by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<Bitmap?>(null) }
    var lastThumb by remember { mutableStateOf<Bitmap?>(null) }
    var thumbVersion by remember { mutableIntStateOf(0) }
    val thumbScale = remember { Animatable(1f) }
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
        lastThumb = withContext(Dispatchers.IO) { latestThumb(context) }
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
                    launch {
                        blink = true
                        delay(110)
                        blink = false
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
                    .clip(RoundedCornerShape(if (portrait) 22.dp else 0.dp))
                    .clipToBounds()
                    .background(Color(0xFF0B0F12))
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
                            color = WizMuted,
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
                            delay(1000)
                        }
                    }

                    overlay?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
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
                        val color = if (level) WizGood else Color.White.copy(alpha = .85f)
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

                // Live GPS chip, top-centre of the preview.
                GpsChip(
                    fix = location.fix,
                    hasLocation = hasLocation,
                    address = location.address,
                    tick = tick,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
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

                // Exposure slider, near the top so it never covers the stamp.
                val cam = camera
                if (showExposure && cam != null && cam.cameraInfo.exposureState.isExposureCompensationSupported) {
                    val es = cam.cameraInfo.exposureState
                    val range = es.exposureCompensationRange
                    val step = es.exposureCompensationStep.toFloat()
                    Row(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 92.dp, start = 20.dp, end = 20.dp)
                            .background(WizGlass, RoundedCornerShape(50))
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
                            colors = SliderDefaults.colors(thumbColor = WizTeal, activeTrackColor = WizTeal),
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
                            .padding(top = 54.dp, end = 12.dp)
                            .background(WizGlass, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Navigation,
                            contentDescription = null,
                            tint = WizTeal,
                            // The arrow points to north on screen.
                            modifier = Modifier.size(13.dp).rotate(-trueHeading)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(StampFormat.heading(trueHeading), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }

                AnimatedVisibility(
                    visible = status.isNotEmpty(),
                    enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { -it / 2 },
                    exit = fadeOut(tween(220)),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        lastStatus,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .background(Color(0xD9091923), RoundedCornerShape(14.dp))
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
                            .background(Color(0x99091923), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (countdown > 0) {
                    CountdownBadge(countdown, Modifier.align(Alignment.Center))
                }

                if (blink) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .6f)))
            }

            // ---- Top bar: quick toggles ---------------------------------------------------------
            val backLens = lens == CameraSelector.LENS_FACING_BACK
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = when (settings.flash) {
                        FlashChoice.OFF -> Icons.Filled.FlashOff
                        FlashChoice.AUTO -> Icons.Filled.FlashAuto
                        FlashChoice.ON -> Icons.Filled.FlashOn
                        FlashChoice.TORCH -> Icons.Filled.FlashlightOn
                    },
                    contentDescription = "Flash ${settings.flash.label}",
                    active = settings.flash != FlashChoice.OFF && backLens,
                    enabled = backLens,
                    onClick = {
                        val next = settings.flash.next()
                        repo.update { it.copy(flash = next) }
                        status = "Flash: ${next.label}"
                    }
                )
                GlassIconButton(
                    icon = when (settings.timerSeconds) {
                        3 -> Icons.Filled.Timer3
                        10 -> Icons.Filled.Timer10
                        else -> Icons.Filled.Timer
                    },
                    contentDescription = "Self-timer",
                    active = settings.timerSeconds > 0,
                    onClick = {
                        val next = when (settings.timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 }
                        repo.update { it.copy(timerSeconds = next) }
                        status = if (next == 0) "Timer off" else "Timer ${next}s"
                    }
                )
                GlassPill(
                    text = settings.aspect.label,
                    onClick = { repo.update { it.copy(aspect = it.aspect.next()) } },
                    modifier = Modifier.widthIn(min = 58.dp)
                )
                GlassIconButton(
                    icon = if (settings.gridOn) Icons.Filled.Grid3x3 else Icons.Filled.GridOff,
                    contentDescription = "Grid",
                    active = settings.gridOn,
                    onClick = { repo.update { it.copy(gridOn = !it.gridOn) } }
                )
                GlassIconButton(
                    icon = Icons.Filled.Straighten,
                    contentDescription = "Level",
                    active = settings.levelOn,
                    onClick = { repo.update { it.copy(levelOn = !it.levelOn) } }
                )
                GlassIconButton(
                    icon = Icons.Filled.Exposure,
                    contentDescription = "Exposure",
                    active = showExposure || exposureIndex != 0,
                    onClick = { showExposure = !showExposure }
                )
                GlassIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    onClick = onSettings
                )
            }

            // ---- Bottom controls ---------------------------------------------------------------
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000), Color.Black)))
                    .navigationBarsPadding()
                    .padding(top = 26.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZoomPresets(camera = camera, zoomRatio = zoomRatio, onSelect = { setZoom(it) })
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassPill(
                        text = activeProject?.name ?: "Project",
                        icon = Icons.Filled.Folder,
                        active = activeProject != null,
                        onClick = { showProjectPicker = true },
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                    GlassPill(
                        text = if (settings.note.isBlank()) "Add note" else settings.note,
                        icon = Icons.Filled.Edit,
                        active = settings.note.isNotBlank(),
                        onClick = { showNoteDialog = true },
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                    GlassPill(
                        text = settings.template.label,
                        icon = Icons.Filled.Layers,
                        onClick = { showStampSheet = true }
                    )
                }
                Spacer(Modifier.height(18.dp))

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 34.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last photo → gallery
                    Box(
                        Modifier
                            .size(54.dp)
                            .scale(thumbScale.value)
                            .clip(RoundedCornerShape(16.dp))
                            .background(WizPanel)
                            .border(2.dp, Color.White.copy(alpha = .85f), RoundedCornerShape(16.dp))
                            .clickable { tickHaptic(); onGallery() },
                        contentAlignment = Alignment.Center
                    ) {
                        val thumb = lastThumb
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = "Open gallery",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Open gallery", tint = Color.White)
                        }
                    }

                    ShutterButton(
                        ready = !busy && imageCapture != null,
                        busy = busy && countdown == 0,
                        burst = settings.burstCount,
                        onClick = shoot
                    )

                    val flipAngle by animateFloatAsState(
                        if (backLens) 0f else 180f,
                        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                        label = "flip"
                    )
                    GlassIconButton(
                        icon = Icons.Filled.Cameraswitch,
                        contentDescription = "Switch camera",
                        size = 54.dp,
                        iconSize = 26.dp,
                        modifier = Modifier.rotate(flipAngle),
                        onClick = {
                            lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                            else CameraSelector.LENS_FACING_BACK
                        }
                    )
                }
            }
        }
    }

    if (showStampSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStampSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WizPanel,
            contentColor = WizText,
            dragHandle = { BottomSheetDefaults.DragHandle(color = WizMuted) }
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
            containerColor = WizPanel,
            title = { Text("Note on photo") },
            text = {
                Column {
                    Text(
                        "Printed on every photo until you change it.",
                        color = WizMuted,
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
                            focusedBorderColor = WizTeal,
                            unfocusedBorderColor = WizLine,
                            cursorColor = WizTeal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (settings.tags.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("Quick tags", color = WizMuted, style = MaterialTheme.typography.labelMedium)
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
                        }) { Text("Clear", color = WizBad) }
                    }
                    TextButton(onClick = { showNoteDialog = false }) { Text("Cancel", color = WizMuted) }
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
@Composable
fun ProjectPickerDialog(
    projects: ProjectStore,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "File new photos under",
    allowNone: Boolean = true
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WizPanel,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Photos are saved in a folder named after the project, and its name is printed on the stamp.",
                    color = WizMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                if (allowNone) ProjectRow("No project", selectedId.isEmpty()) { onSelect("") }
                projects.projects.sortedByDescending { it.createdMs }.forEach { p ->
                    ProjectRow(p.name, p.id == selectedId) { onSelect(p.id) }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(60) },
                        singleLine = true,
                        placeholder = { Text("New project") },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WizTeal,
                            unfocusedBorderColor = WizLine,
                            cursorColor = WizTeal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Create project",
                        enabled = newName.isNotBlank(),
                        active = newName.isNotBlank(),
                        onClick = { onSelect(projects.create(newName).id) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = WizMuted) } }
    )
}

@Composable
private fun ProjectRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val tick = rememberTick()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) WizTeal.copy(alpha = .16f) else Color.Transparent)
            .clickable { tick(); onClick() }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, tint = if (selected) WizTeal else WizMuted)
        Spacer(Modifier.width(12.dp))
        Text(name, color = WizText, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ---- Pieces ---------------------------------------------------------------------------------

@Composable
private fun ShutterButton(ready: Boolean, busy: Boolean, burst: Int, onClick: () -> Unit) {
    val tick = rememberTick()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val inner by animateFloatAsState(
        if (pressed) 0.80f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "shutter"
    )
    Box(
        Modifier
            .size(84.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = ready,
                onClick = { tick(); onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize().border(4.dp, Color.White, CircleShape))
        Box(
            Modifier
                .size(66.dp)
                .scale(inner)
                .clip(CircleShape)
                .background(if (ready || busy) Color.White else Color(0xFF7D8A94))
        )
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = WizTeal,
                strokeWidth = 4.dp
            )
        } else if (burst > 1) {
            Text("×$burst", color = WizNavy, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun FocusRing(p: Offset) {
    val appear = remember(p) { Animatable(1.5f) }
    LaunchedEffect(p) { appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)) }
    Box(
        Modifier
            .offset { IntOffset((p.x - 36.dp.toPx()).toInt(), (p.y - 36.dp.toPx()).toInt()) }
            .size(72.dp)
            .scale(appear.value)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(WizTeal, radius = size.minDimension / 2f - 2f, style = Stroke(width = 2.5f * density))
            drawCircle(WizTeal, radius = 3f * density)
        }
    }
}

@Composable
private fun CountdownBadge(value: Int, modifier: Modifier = Modifier) {
    val pop = remember(value) { Animatable(1.6f) }
    LaunchedEffect(value) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)) }
    Box(
        modifier
            .size(132.dp)
            .scale(pop.value)
            .clip(CircleShape)
            .background(Color(0x99091923))
            .border(2.dp, WizTeal.copy(alpha = .8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(value.toString(), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GpsChip(
    fix: GpsFix?,
    hasLocation: Boolean,
    address: String?,
    tick: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Reading `tick` makes this recompose every second so the colour follows the fix age.
    @Suppress("UNUSED_VARIABLE") val unused = tick
    val color = gpsColor(fix, hasLocation)
    val pulse = rememberInfiniteTransition(label = "gpsPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "gpsPulseAlpha"
    )
    val place = address?.lineSequence()?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    val text = when {
        !hasLocation -> "Location is off"
        fix == null -> "Searching GPS…"
        fix.isMock -> "Mock location detected"
        else -> {
            val acc = fix.accuracyM?.let { "±${it.toInt()} m" } ?: "GPS"
            val age = fix.ageSeconds()
            when {
                age > 10 -> "$acc · ${age}s ago"
                place != null -> "$acc · $place"
                else -> "$acc · Live"
            }
        }
    }
    val tickHaptic = rememberTick()
    androidx.compose.material3.Surface(
        onClick = { tickHaptic(); onClick() },
        shape = RoundedCornerShape(50),
        color = WizGlass,
        contentColor = Color.White,
        border = BorderStroke(1.dp, color.copy(alpha = .45f)),
        modifier = modifier.widthIn(max = 260.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(if (color == WizTeal) pulseAlpha else 1f)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ZoomPresets(camera: Camera?, zoomRatio: Float, onSelect: (Float) -> Unit) {
    val zs = camera?.cameraInfo?.zoomState?.value
    val minZ = zs?.minZoomRatio ?: 1f
    val maxZ = zs?.maxZoomRatio ?: 1f
    val presets = buildList {
        if (minZ < 0.95f) add(minZ)
        add(1f)
        if (maxZ >= 2f) add(2f)
        if (maxZ >= 5f) add(5f)
    }
    if (presets.size < 2) {
        Spacer(Modifier.height(34.dp))
        return
    }
    // The chip at or below the current zoom is "active"; it shows the exact value while pinching.
    val activeIndex = presets.indexOfLast { it <= zoomRatio + 0.01f }.coerceAtLeast(0)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(WizGlass)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEachIndexed { i, p ->
            val active = i == activeIndex
            val label = if (active) zoomLabel(zoomRatio) else zoomLabel(p)
            val scale by animateFloatAsState(if (active) 1f else 0.86f, tween(160), label = "zoomChip")
            val tick = rememberTick()
            Box(
                Modifier
                    .size(if (active) 40.dp else 34.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (active) Color(0x33FFFFFF) else Color.Transparent)
                    .clickable { tick(); onSelect(p) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) WizTeal else Color.White,
                    fontSize = if (active) 13.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StampSheet(settings: AppSettings, repo: SettingsRepo, onMore: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 22.dp, end = 22.dp, bottom = 20.dp)
    ) {
        Text("Stamp style", style = MaterialTheme.typography.titleLarge, color = WizText)
        Text("Changes appear live on the camera.", style = MaterialTheme.typography.bodySmall, color = WizMuted)
        Spacer(Modifier.height(8.dp))
        OptionGroup(
            label = "Layout",
            icon = Icons.Filled.Layers,
            options = StampTemplate.values().map { it.label to it },
            selected = settings.template
        ) { v -> repo.update { it.copy(template = v) } }
        OptionGroup(
            label = "Position",
            icon = Icons.Filled.VerticalAlignBottom,
            options = StampPosition.values().map { it.label to it },
            selected = settings.position
        ) { v -> repo.update { it.copy(position = v) } }
        OptionGroup(
            label = "Style",
            icon = Icons.Filled.Palette,
            options = StampStyle.values().map { it.label to it },
            selected = settings.style
        ) { v -> repo.update { it.copy(style = v) } }

        Spacer(Modifier.height(6.dp))
        Text("Show on photo", style = MaterialTheme.typography.bodyLarge, color = WizText)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillChip("Map", settings.showMap) { repo.update { it.copy(showMap = !it.showMap) } }
            PillChip("Address", settings.showAddress) { repo.update { it.copy(showAddress = !it.showAddress) } }
            PillChip("Coordinates", settings.showCoords) { repo.update { it.copy(showCoords = !it.showCoords) } }
            PillChip("Date & time", settings.showDateTime) { repo.update { it.copy(showDateTime = !it.showDateTime) } }
            PillChip("Weather", settings.showWeather) { repo.update { it.copy(showWeather = !it.showWeather) } }
            PillChip("Altitude", settings.showAltitude) { repo.update { it.copy(showAltitude = !it.showAltitude) } }
            PillChip("Compass", settings.showHeading) { repo.update { it.copy(showHeading = !it.showHeading) } }
            PillChip("Accuracy", settings.showAccuracy) { repo.update { it.copy(showAccuracy = !it.showAccuracy) } }
            PillChip("Speed", settings.showSpeed) { repo.update { it.copy(showSpeed = !it.showSpeed) } }
            PillChip("Plus code", settings.showPlusCode) { repo.update { it.copy(showPlusCode = !it.showPlusCode) } }
            PillChip("Logo", settings.showLogo) { repo.update { it.copy(showLogo = !it.showLogo) } }
        }

        Spacer(Modifier.height(18.dp))
        TextButton(
            onClick = onMore,
            colors = ButtonDefaults.textButtonColors(contentColor = WizTeal),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("All stamp settings")
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
