package com.wizlabs.gpscamera

import android.Manifest
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import org.osmdroid.views.MapView
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.min

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

@Composable
fun CameraScreen(
    repo: SettingsRepo,
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

    var hasCamera by remember { mutableStateOf(isGranted(context, Manifest.permission.CAMERA)) }
    var hasLocation by remember {
        mutableStateOf(
            isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flash by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var zoomText by remember { mutableStateOf<String?>(null) }
    var zoomStamp by remember { mutableLongStateOf(0L) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var blink by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<Bitmap?>(null) }
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
            delay(3500)
            status = ""
        }
    }

    LaunchedEffect(zoomStamp) {
        delay(1300)
        zoomText = null
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1200)
            focusPoint = null
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
            imageCapture = ic
        } catch (e: Exception) {
            status = "Camera unavailable: ${e.localizedMessage}"
        }
    }

    DisposableEffect(Unit) {
        onDispose { providerHolder[0]?.unbindAll() }
    }

    LaunchedEffect(imageCapture, flash, lens) {
        imageCapture?.flashMode =
            if (flash && lens == CameraSelector.LENS_FACING_BACK) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
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
                    val data = buildStampData(
                        s, fix, location.address, location.addressCached, location.weather,
                        sensors.headingDeg, location.declination, newPhotoId()
                    )
                    val mapCopy = snapshotMap(mapView, null)
                    if (s.vibrate) Haptics.shutter(context)
                    launch {
                        blink = true
                        delay(120)
                        blink = false
                    }
                    val res = suspendCancellableCoroutine<Result<PhotoPipeline.Saved>> { c ->
                        PhotoPipeline.capture(context, ic, data, s, mapCopy, logo) { c.resume(it) }
                    }
                    if (res.isSuccess) {
                        saved++
                    } else {
                        status = "Capture failed: ${res.exceptionOrNull()?.localizedMessage}"
                        break
                    }
                    if (i < s.burstCount - 1) delay(300)
                }
                if (saved == 1) status = "Photo saved" else if (saved > 1) status = "$saved photos saved"
            } finally {
                busy = false
                countdown = 0
            }
        }
        Unit
    }

    // ---- UI ---------------------------------------------------------------------------------
    Box(Modifier.fillMaxSize().background(WizBar)) {
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
        Box(Modifier.fillMaxSize().background(WizBar))

        Column(Modifier.fillMaxSize()) {
            // ---- Header: logo, title, settings -------------------------------------------------
            Column(Modifier.fillMaxWidth().background(WizBar).statusBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp)) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                buildAnnotatedString {
                                    append("Wiz ")
                                    withStyle(SpanStyle(color = WizTeal)) { append("GPS") }
                                    append(" Camera")
                                },
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Wiz Labs", color = Color(0xFF9AA7B2), fontSize = 13.sp)
                        }
                    }
                    IconButton(
                        onClick = onSettings,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, WizTeal, Color.Transparent)))
                )
            }

            // ---- Quick row: flash, aspect ratio, GPS, grid -----------------------------------
            val backLens = lens == CameraSelector.LENS_FACING_BACK
            Row(
                Modifier.fillMaxWidth().background(WizBar).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { flash = !flash }, enabled = backLens) {
                    Icon(
                        Icons.Filled.FlashOn,
                        contentDescription = "Flash",
                        tint = when {
                            !backLens -> Color.Gray
                            flash -> WizTeal
                            else -> Color.White
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
                Box(
                    Modifier
                        .size(width = 64.dp, height = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                        .clickable { repo.update { it.copy(aspect = it.aspect.next()) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(settings.aspect.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                GpsPinButton(location.fix, hasLocation, tick) {
                    if (hasLocation) {
                        val f = location.fix
                        status = if (f == null) "Searching GPS…" else
                            "GPS " + (f.accuracyM?.let { "±${it.toInt()} m · " } ?: "") + "${f.ageSeconds()}s ago"
                        location.start()
                        location.refresh()
                    } else {
                        permLauncher.launch(CAMERA_PERMS)
                    }
                }
                IconButton(onClick = { repo.update { it.copy(gridOn = !it.gridOn) } }) {
                    Icon(
                        Icons.Filled.Grid3x3,
                        contentDescription = "Grid",
                        tint = if (settings.gridOn) WizTeal else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // ---- Preview (shaped by the chosen aspect ratio) -----------------------------------
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().background(WizBar),
                contentAlignment = Alignment.Center
            ) {
                val portrait = maxHeight >= maxWidth
                val ratio = if (portrait) 1f / settings.aspect.ratio else settings.aspect.ratio
                Box(Modifier.aspectRatio(ratio).clipToBounds().background(Color.Black)) {
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
                            Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("WIZ GPS CAMERA", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Allow camera access to take photos.", color = Color.LightGray)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { permLauncher.launch(CAMERA_PERMS) }) { Text("Grant permissions") }
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
                                    location.weather, sensors.headingDeg, location.declination, "WGC-LIVE"
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
                            val c = Color.White.copy(alpha = .35f)
                            for (i in 1..2) {
                                val x = size.width * i / 3f
                                val y = size.height * i / 3f
                                drawLine(c, Offset(x, 0f), Offset(x, size.height), 1.5f)
                                drawLine(c, Offset(0f, y), Offset(size.width, y), 1.5f)
                            }
                        }
                    }

                    if (settings.levelOn && sensors.levelValid) {
                        Canvas(Modifier.fillMaxSize()) {
                            val dev = sensors.levelDeg
                            val level = abs(dev) < 1f
                            val color = if (level) Color(0xFF66FF99) else Color.White.copy(alpha = .85f)
                            val cy = size.height / 2f
                            drawLine(color.copy(alpha = .5f), Offset(size.width * .42f, cy), Offset(size.width * .46f, cy), 3f)
                            drawLine(color.copy(alpha = .5f), Offset(size.width * .54f, cy), Offset(size.width * .58f, cy), 3f)
                            rotate(dev) {
                                drawLine(color, Offset(size.width * .25f, cy), Offset(size.width * .75f, cy), 5f, cap = StrokeCap.Round)
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
                                    zoomText = String.format(Locale.US, "%.1fx", target)
                                    zoomStamp = System.nanoTime()
                                }
                            }
                    )

                    focusPoint?.let { p ->
                        Box(
                            Modifier
                                .offset { IntOffset((p.x - 36.dp.toPx()).toInt(), (p.y - 36.dp.toPx()).toInt()) }
                                .size(72.dp)
                                .border(2.dp, WizTeal, CircleShape)
                        )
                    }

                    // Small note pill, top-left of the preview.
                    Surface(
                        onClick = { showNoteDialog = true },
                        shape = RoundedCornerShape(50),
                        color = Color(0x80091923),
                        contentColor = Color.White,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit note", modifier = Modifier.size(14.dp))
                            if (settings.note.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    settings.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }
                    }

                    zoomText?.let {
                        Text(
                            it,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color(0x99091923), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (countdown > 0) {
                        Text(
                            countdown.toString(),
                            color = Color.White,
                            fontSize = 96.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    if (status.isNotEmpty()) {
                        Text(
                            status,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .background(Color(0xB3091923), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (blink) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .55f)))
                }
            }

            // ---- Bottom bar: gallery, shutter, switch camera -----------------------------------
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(WizBar)
                    .navigationBarsPadding()
                    .padding(horizontal = 36.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onGallery, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                val shutterReady = !busy && imageCapture != null
                Box(
                    Modifier
                        .size(84.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(7.dp)
                        .clip(CircleShape)
                        .background(if (shutterReady) Color.White else Color(0xFF8D99A3))
                        .clickable(enabled = shutterReady, onClick = shoot)
                )
                IconButton(
                    onClick = {
                        lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
                        else CameraSelector.LENS_FACING_BACK
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Cached,
                            contentDescription = "Switch camera",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                        Box(Modifier.size(12.dp).background(WizTeal, CircleShape))
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        var draft by remember { mutableStateOf(settings.note) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Note on photo") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    maxLines = 3,
                    label = { Text("e.g. Inspection – Pump 2") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    repo.update { it.copy(note = draft.trim()) }
                    showNoteDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNoteDialog = false }) { Text("Cancel") } }
        )
    }
}

/** Pin icon whose colour shows GPS health: teal = fresh fix, amber = ageing, red = stale/off/mock. */
@Composable
private fun GpsPinButton(fix: GpsFix?, hasLocation: Boolean, tick: Long, onClick: () -> Unit) {
    // Reading `tick` makes this recompose every second so the colour follows the fix age.
    @Suppress("UNUSED_VARIABLE") val unused = tick
    val color = when {
        !hasLocation -> Color(0xFFFF5252)
        fix == null -> Color(0xFFFFB74D)
        fix.isMock -> Color(0xFFFF5252)
        else -> {
            val age = fix.ageSeconds()
            when {
                age <= 10 -> WizTeal
                age <= 60 -> Color(0xFFFFB74D)
                else -> Color(0xFFFF5252)
            }
        }
    }
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.LocationOn, contentDescription = "GPS status", tint = color, modifier = Modifier.size(32.dp))
    }
}
