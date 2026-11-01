package com.wizlabs.gpscamera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

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
    val address: String = "Waiting for location…",
    val accuracy: Float? = null
) {
    val coordinateText: String
        get() = if (lat != null && lon != null)
            String.format(Locale.US, "%.6f, %.6f", lat, lon) else "Coordinates unavailable"
}

@Composable
private fun WizGpsCamera() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasCamera by remember { mutableStateOf(false) }
    var hasLocation by remember { mutableStateOf(false) }
    var gps by remember { mutableStateOf(GpsInfo()) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flash by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Preparing camera…") }

    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
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
        if (!cameraOk || !fineOk && !coarseOk) {
            permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else refreshLocation(context) { gps = it }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFFB7DCE5), background = Color(0xFF090E12),
        surface = Color(0xFF111A20), onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(Color(0xFF090E12))) {
            if (hasCamera) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                                .build()
                            capture = imageCapture
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, CameraSelector.Builder().requireLensFacing(lens).build(), preview, imageCapture)
                                status = "Camera ready"
                            } catch (e: Exception) { status = "Unable to start camera: ${e.localizedMessage}" }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    update = { view ->
                        val providerFuture = ProcessCameraProvider.getInstance(context)
                        providerFuture.addListener({
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                                .build()
                            capture = imageCapture
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, CameraSelector.Builder().requireLensFacing(lens).build(), preview, imageCapture)
                            } catch (_: Exception) { status = "Camera unavailable" }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
            } else {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WIZ GPS CAMERA", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Camera permission is required to capture photos.", color = Color.LightGray)
                    Button(onClick = { permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Grant permissions") }
                }
            }

            // Compact map/address card. OSMDroid uses its local tile cache when offline.
            Column(
                Modifier.align(Alignment.TopStart).padding(16.dp)
                    .fillMaxWidth(0.82f)
                    .background(Color(0xD910171D), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFB7DCE5))
                    Spacer(Modifier.width(6.dp))
                    Text(gps.address, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(5.dp))
                Text(SimpleDateFormat("dd MMM yyyy · hh:mm:ss a", Locale.getDefault()).format(Date()), color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.labelMedium)
                Text(gps.coordinateText, color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelSmall)
                Text(gps.accuracy?.let { "GPS accuracy ±${it.toInt()} m" } ?: "Waiting for GPS signal", color = Color(0xFFB7DCE5), style = MaterialTheme.typography.labelSmall)
            }

            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xCC090E12)).padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { status = "Gallery access will be enabled in the next build." }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                }
                IconButton(onClick = { flash = !flash }) {
                    Icon(if (flash) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Flash", tint = Color.White)
                }
                Button(
                    onClick = { takePhoto(context, capture, gps) { status = it } },
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) { Box(Modifier.size(58.dp).background(Color.Transparent, CircleShape)) }
                IconButton(onClick = { lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }) {
                    Icon(Icons.Default.SwitchCamera, contentDescription = "Switch camera", tint = Color.White)
                }
                IconButton(onClick = { refreshLocation(context) { gps = it } }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Refresh GPS", tint = Color.White)
                }
            }

            Text(status, Modifier.align(Alignment.BottomCenter).padding(bottom = 108.dp), color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall)
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

private fun takePhoto(context: android.content.Context, imageCapture: ImageCapture?, gps: GpsInfo, report: (String) -> Unit) {
    if (imageCapture == null) { report("Camera is not ready"); return }
    val temp = File(context.cacheDir, "wiz_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(temp).build()
    imageCapture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onError(exception: ImageCaptureException) { report("Capture failed: ${exception.message}") }
        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(temp.absolutePath)
                val stamped = stampPhoto(bitmap, gps)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "WizGPS_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wiz GPS Camera")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        stamped.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    report("Photo saved to Pictures/Wiz GPS Camera")
                } else report("Unable to save photograph")
                temp.delete()
            } catch (e: Exception) { report("Photo processing failed: ${e.localizedMessage}") }
        }
    })
}

private fun stampPhoto(source: Bitmap, gps: GpsInfo): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val scale = result.width / 1080f
    val pad = 24f * scale
    val boxWidth = result.width * .82f
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 26f * scale; typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL) }
    val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF1.toInt(); textSize = 21f * scale }
    val boxHeight = 150f * scale
    val left = pad
    val top = result.height - boxHeight - pad
    val rect = RectF(left, top, left + boxWidth, top + boxHeight)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD910171D.toInt() }
    canvas.drawRoundRect(rect, 22f * scale, 22f * scale, bg)
    val address = gps.address.take(72)
    canvas.drawText(address, left + 20f * scale, top + 38f * scale, linePaint)
    canvas.drawText(SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault()).format(Date()), left + 20f * scale, top + 76f * scale, smallPaint)
    canvas.drawText(gps.coordinateText, left + 20f * scale, top + 112f * scale, smallPaint)
    return result
}
