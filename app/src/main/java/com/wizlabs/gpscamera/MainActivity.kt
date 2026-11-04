package com.wizlabs.gpscamera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Geocoder
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF091923)
private val Teal = Color(0xFF70DCE8)
private val Glass = Color(0xD9162832)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContent { WizGpsCamera() }
    }
}

private data class GpsInfo(
    val lat: Double? = null,
    val lon: Double? = null,
    val address: String = "Waiting for GPS location",
    val accuracy: Float? = null
) {
    val coordinateText: String
        get() = if (lat != null && lon != null)
            String.format(Locale.US, "%.6f, %.6f", lat, lon) else "Coordinates unavailable"
    val hasFix: Boolean get() = lat != null && lon != null
}

@Composable
private fun WizGpsCamera() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasCamera by remember { mutableStateOf(false) }
    var hasLocation by remember { mutableStateOf(false) }
    var gps by remember { mutableStateOf(GpsInfo()) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flash by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Starting camera…") }
    var captureBusy by remember { mutableStateOf(false) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasCamera = result[Manifest.permission.CAMERA] == true
        hasLocation = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocation) refreshLocation(context) { gps = it }
    }

    LaunchedEffect(Unit) {
        val cameraOk = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fineOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasCamera = cameraOk
        hasLocation = fineOk || coarseOk
        if (!cameraOk || !hasLocation) {
            permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else refreshLocation(context) { gps = it }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView?.onPause()
            mapView?.onDetach()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Teal, background = Navy, surface = Color(0xFF132630), onSurface = Color.White)) {
        Box(Modifier.fillMaxSize().background(Navy)) {
            if (hasCamera) {
                key(lens, flash) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                                // Cap still-photo capture at 12 megapixels. Keep the largest
                                // camera-supported output at or below 12 MP (rather than
                                // requesting an exact 4:3 size that some devices may not offer).
                                val photoResolutionSelector = ResolutionSelector.Builder()
                                    .setResolutionFilter { supportedSizes, _ ->
                                        val atOrBelow12Mp = supportedSizes.filter {
                                            it.width.toLong() * it.height.toLong() <= 12_000_000L
                                        }
                                        (if (atOrBelow12Mp.isNotEmpty()) atOrBelow12Mp else supportedSizes)
                                            .sortedByDescending { it.width.toLong() * it.height.toLong() }
                                    }
                                    .build()
                                val imageCapture = ImageCapture.Builder()
                                    .setResolutionSelector(photoResolutionSelector)
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setFlashMode(if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                                    .build()
                                capture = imageCapture
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.Builder().requireLensFacing(lens).build(), preview, imageCapture)
                                    status = "Camera ready"
                                } catch (e: Exception) { status = "Camera unavailable: ${e.localizedMessage}" }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                )
                }
            } else {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WIZ GPS CAMERA", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Allow camera access to take photos.", color = Color.LightGray)
                    Button(onClick = { permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) {
                        Text("Grant permissions")
                    }
                }
            }

            // Live geo-tag: bottom-centre, translucent glass, with a real OSM mini-map.
            GeoTagOverlay(
                gps = gps,
                onMapReady = { mapView = it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 126.dp)
            )

            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xE609141B)).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { status = "Gallery is not implemented in this build." }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                }
                IconButton(onClick = { flash = !flash; status = "Flash setting updated" }) {
                    Icon(if (flash) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Flash", tint = Color.White)
                }
                Button(
                    enabled = !captureBusy && capture != null,
                    onClick = {
                        captureBusy = true
                        takePhoto(context, capture, gps, mapView) {
                            status = it
                            captureBusy = false
                        }
                    },
                    modifier = Modifier.size(76.dp).border(3.dp, Color.White.copy(alpha = .8f), CircleShape),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Navy)
                ) {
                    Box(Modifier.size(58.dp).border(2.dp, Navy.copy(alpha = .55f), CircleShape))
                }
                IconButton(onClick = {
                    lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                }) {
                    Icon(Icons.Default.SwitchCamera, contentDescription = "Switch camera", tint = Color.White)
                }
                IconButton(onClick = {
                    if (hasLocation) refreshLocation(context) { gps = it }
                    else status = "Location permission is not granted"
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Refresh GPS", tint = Teal)
                }
            }

            Text(status, Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp), color = Color.White.copy(alpha = .88f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GeoTagOverlay(gps: GpsInfo, onMapReady: (MapView) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Glass)
            .border(1.dp, Color.White.copy(alpha = .22f), RoundedCornerShape(22.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            modifier = Modifier.width(142.dp).height(126.dp).clip(RoundedCornerShape(15.dp)),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(false)
                    controller.setZoom(15.5)
                    setUseDataConnection(true)
                    if (gps.hasFix) controller.setCenter(GeoPoint(gps.lat!!, gps.lon!!))
                    onMapReady(this)
                }
            },
            update = { map ->
                if (gps.hasFix) {
                    val point = GeoPoint(gps.lat!!, gps.lon!!)
                    map.overlays.removeAll { it is Marker }
                    map.controller.setCenter(point)
                    val marker = Marker(map).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Photo location"
                    }
                    map.overlays.add(marker)
                    map.invalidate()
                }
            }
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Teal, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("WIZ GPS CAMERA", color = Teal, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Text(
                gps.address,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(gps.coordinateText, color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(
                SimpleDateFormat("dd MMM yyyy · hh:mm:ss a", Locale.getDefault()).format(Date()),
                color = Color.White.copy(alpha = .86f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                gps.accuracy?.let { "GPS ±${it.toInt()} m" } ?: "Waiting for GPS fix",
                color = Teal,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun refreshLocation(context: android.content.Context, onResult: (GpsInfo) -> Unit) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                onResult(GpsInfo(address = "Waiting for GPS fix"))
                return@addOnSuccessListener
            }
            var info = GpsInfo(location.latitude, location.longitude, "Resolving address…", location.accuracy)
            onResult(info)
            try {
                if (Geocoder.isPresent()) {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        val a = addresses.firstOrNull()
                        val line = listOfNotNull(a?.thoroughfare, a?.subLocality, a?.locality, a?.adminArea)
                            .distinct().joinToString(", ")
                        info = info.copy(address = line.ifBlank { "Address unavailable" })
                        onResult(info)
                    }
                } else onResult(info.copy(address = "Address unavailable"))
            } catch (_: Exception) { onResult(info.copy(address = "Address unavailable")) }
        }.addOnFailureListener { onResult(GpsInfo(address = "GPS unavailable")) }
    } catch (_: SecurityException) { onResult(GpsInfo(address = "Location permission needed")) }
}

private fun takePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture?,
    gps: GpsInfo,
    mapView: MapView?,
    report: (String) -> Unit
) {
    if (imageCapture == null) { report("Camera is not ready"); return }
    val temp = java.io.File(context.cacheDir, "wiz_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(temp).build()
    imageCapture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onError(exception: ImageCaptureException) { report("Capture failed: ${exception.message}") }
        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
            try {
                val source = android.graphics.BitmapFactory.decodeFile(temp.absolutePath)
                    ?: throw IllegalStateException("Could not decode captured photo")
                val mapBitmap = mapView?.let { view ->
                    if (view.width > 0 && view.height > 0) {
                        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { bmp ->
                            view.draw(Canvas(bmp))
                        }
                    } else null
                }
                val stamped = stampPhoto(source, gps, mapBitmap)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "WizGPS_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wiz GPS Camera")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Unable to create photo in gallery")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    if (!stamped.compress(Bitmap.CompressFormat.JPEG, 95, out)) throw IllegalStateException("Image encoding failed")
                } ?: throw IllegalStateException("Unable to write photo")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                report("Photo saved to Pictures/Wiz GPS Camera")
                if (stamped !== source) stamped.recycle()
                source.recycle()
                mapBitmap?.recycle()
                temp.delete()
            } catch (e: Exception) {
                temp.delete()
                report("Photo processing failed: ${e.localizedMessage}")
            }
        }
    })
}

private fun stampPhoto(source: Bitmap, gps: GpsInfo, mapBitmap: Bitmap?): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val scale = result.width / 1080f
    val margin = 24f * scale
    val panelWidth = result.width * .92f
    val panelHeight = 236f * scale
    val left = (result.width - panelWidth) / 2f
    val top = result.height - panelHeight - margin
    val radius = 24f * scale

    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55000000
        setShadowLayer(10f * scale, 0f, 4f * scale, 0x66000000)
    }
    val panel = RectF(left, top, left + panelWidth, top + panelHeight)
    canvas.drawRoundRect(panel, radius, radius, shadow)
    val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD9162832.toInt() }
    canvas.drawRoundRect(panel, radius, radius, glass)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * scale
    }
    canvas.drawRoundRect(panel, radius, radius, border)

    val mapSize = 174f * scale
    val mapRect = RectF(left + 14f * scale, top + 14f * scale, left + 14f * scale + mapSize, top + 14f * scale + mapSize)
    if (mapBitmap != null) {
        val mapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.save()
        val path = android.graphics.Path().apply { addRoundRect(mapRect, 15f * scale, 15f * scale, android.graphics.Path.Direction.CW) }
        canvas.clipPath(path)
        canvas.drawBitmap(mapBitmap, null, mapRect, mapPaint)
        canvas.restore()
    } else {
        val fallback = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF233D49.toInt() }
        canvas.drawRoundRect(mapRect, 15f * scale, 15f * scale, fallback)
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x5570DCE8; strokeWidth = 2f * scale }
        for (i in 1..3) {
            val x = mapRect.left + mapRect.width() * i / 4f
            val y = mapRect.top + mapRect.height() * i / 4f
            canvas.drawLine(x, mapRect.top, x, mapRect.bottom, grid)
            canvas.drawLine(mapRect.left, y, mapRect.right, y, grid)
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF70DCE8.toInt() }
        canvas.drawCircle(mapRect.centerX(), mapRect.centerY(), 8f * scale, dot)
    }

    val textLeft = mapRect.right + 16f * scale
    val availableWidth = panel.right - textLeft - 14f * scale
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF70DCE8.toInt()
        textSize = 19f * scale
        typeface = Typeface.create("sans-serif-bold", Typeface.NORMAL)
    }
    val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 26f * scale
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val detail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF1.toInt()
        textSize = 19f * scale
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    var y = top + 39f * scale
    canvas.drawText("WIZ GPS CAMERA", textLeft, y, title)
    y += 43f * scale
    drawWrappedText(canvas, gps.address, textLeft, y, availableWidth, addressPaint, 31f * scale, 2)
    y += 75f * scale
    val date = SimpleDateFormat("dd MMM yyyy · hh:mm:ss a", Locale.getDefault()).format(Date())
    canvas.drawText(date, textLeft, y, detail)
    y += 34f * scale
    canvas.drawText(gps.coordinateText, textLeft, y, detail)
    y += 31f * scale
    canvas.drawText(gps.accuracy?.let { "GPS accuracy ±${it.toInt()} m" } ?: "GPS location unavailable", textLeft, y, detail)
    return result
}

private fun drawWrappedText(canvas: Canvas, text: String, x: Float, baseline: Float, maxWidth: Float, paint: Paint, lineHeight: Float, maxLines: Int) {
    val words = text.split(Regex("\\s+"))
    var line = ""
    var y = baseline
    var count = 0
    for (word in words) {
        val candidate = if (line.isEmpty()) word else "$line $word"
        if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
            count++
            if (count >= maxLines) return
            line = word
            y += lineHeight
        } else line = candidate
    }
    if (line.isNotEmpty() && count < maxLines) canvas.drawText(line, x, y, paint)
}
