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
import android.content.Intent
import android.database.Cursor
import android.widget.ImageView
import android.os.Bundle
import android.os.Build
import android.location.Location
import android.graphics.Matrix
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import android.util.Size
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
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
import java.util.concurrent.Executors

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
    val accuracy: Float? = null,
    val fixTimeMillis: Long? = null,
    val altitude: Double? = null,
    val speedMps: Float? = null,
    val bearing: Float? = null,
    val isMock: Boolean = false
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
    var showGallery by remember { mutableStateOf(false) }
    var galleryItems by remember { mutableStateOf(listOf<GalleryPhoto>()) }
    var lastSavedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

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
            if (showGallery) {
                GalleryScreen(
                    photos = galleryItems,
                    onBack = { showGallery = false },
                    onRefresh = { galleryItems = loadGallery(context) },
                    onShare = { sharePhoto(context, it.uri) },
                    onDelete = { photo ->
                        try { context.contentResolver.delete(photo.uri, null, null) } catch (_: Exception) {}
                        galleryItems = loadGallery(context)
                    }
                )
            } else if (hasCamera) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            val provider = try { providerFuture.get() } catch (e: Exception) { status = "Camera provider failed: ${e.localizedMessage}"; return@addListener }
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(4032, 3024), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER)).build())
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
                            val provider = try { providerFuture.get() } catch (e: Exception) { status = "Camera provider failed: ${e.localizedMessage}"; return@addListener }
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(4032, 3024), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER)).build())
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

            // GPS Map Camera-style lower watermark: map thumbnail on the left and geo details on the right.
            if (!showGallery) {
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 112.dp)
                        .background(Color(0xD910171D), RoundedCornerShape(12.dp)).padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(94.dp).height(112.dp).background(Color(0xFF25313A), RoundedCornerShape(8.dp))) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    mapViewRef = this
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(false)
                                    controller.setZoom(if (gps.lat != null) 15.0 else 3.0)
                                    if (gps.lat != null && gps.lon != null) {
                                        val point = GeoPoint(gps.lat, gps.lon)
                                        controller.setCenter(point)
                                        overlays.add(Marker(this).apply { position = point; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); title = gps.address })
                                    }
                                }
                            },
                            update = { map ->
                                if (gps.lat != null && gps.lon != null) {
                                    val point = GeoPoint(gps.lat, gps.lon)
                                    map.controller.setCenter(point)
                                    map.overlays.removeAll { it is Marker }
                                    map.overlays.add(Marker(map).apply { position = point; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) })
                                    map.invalidate()
                                }
                            }
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Wiz GPS Camera", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(gps.address, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(gps.coordinateText, color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodySmall)
                        Text(SimpleDateFormat("EEE, dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(Date()), color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.bodySmall)
                        val fixTime = gps.fixTimeMillis
                        val age = fixTime?.let { ((System.currentTimeMillis() - it).coerceAtLeast(0L) / 1000L) }
                        Text(gps.accuracy?.let { "● Fix ±${it.toInt()} m · ${age ?: "?"} s old" } ?: "● Waiting for GPS", color = if (age != null && age < 15) Color(0xFF66DD99) else Color(0xFFFFC36B), style = MaterialTheme.typography.labelSmall)
                        if (gps.isMock) Text("⚠ MOCK LOCATION", color = Color(0xFFFF7777), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (!showGallery) Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xCC090E12)).padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { galleryItems = loadGallery(context); showGallery = true }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                    }
                    Text("Gallery", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(enabled = lastSavedPhotoUri != null, onClick = { lastSavedPhotoUri?.let { sharePhoto(context, it) } }) {
                        Icon(Icons.Default.Share, contentDescription = "Share latest photo to WhatsApp or other apps", tint = if (lastSavedPhotoUri != null) Color(0xFFB7DCE5) else Color.Gray)
                    }
                    Text("Share", color = if (lastSavedPhotoUri != null) Color.White.copy(alpha = .7f) else Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { flash = !flash }) {
                    Icon(if (flash) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Flash", tint = Color.White)
                }
                Button(
                    onClick = {
                        val mapSnapshot = try {
                            mapViewRef?.let { map ->
                                if (map.width > 0 && map.height > 0) Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888).also { bmp -> map.draw(Canvas(bmp)) }
                                else null
                            }
                        } catch (_: Exception) { null }
                        takePhoto(context, capture, gps, mapSnapshot, report = { status = it }, onPhotoSaved = { uri -> lastSavedPhotoUri = uri; galleryItems = loadGallery(context) })
                    },
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

            if (!showGallery) Text(status, Modifier.align(Alignment.BottomCenter).padding(bottom = 108.dp), color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}


private data class GalleryPhoto(val uri: Uri, val name: String)

private fun loadGallery(context: android.content.Context): List<GalleryPhoto> {
    val items = mutableListOf<GalleryPhoto>()
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
    val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    try {
        context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: ""
                if (name.startsWith("WizGPS_")) {
                    val id = cursor.getLong(idCol)
                    items.add(GalleryPhoto(Uri.withAppendedPath(collection, id.toString()), name))
                }
            }
        }
    } catch (_: Exception) {}
    return items
}

private fun sharePhoto(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, "Wiz GPS Camera photo", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try { context.startActivity(Intent.createChooser(intent, "Share original photo")) } catch (_: Exception) {}
}

@Composable
private fun GalleryScreen(
    photos: List<GalleryPhoto>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onShare: (GalleryPhoto) -> Unit,
    onDelete: (GalleryPhoto) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color(0xFF090E12)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("Wiz GPS Camera", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Gallery · ${photos.size} photos", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }
        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No photos yet\nCapture a photo to see it here.", color = Color.LightGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos, key = { it.uri.toString() }) { photo ->
                    Column(Modifier.background(Color(0xFF152029), RoundedCornerShape(10.dp)).padding(6.dp)) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(190.dp).clickable { onShare(photo) },
                            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                            update = { image -> image.setImageURI(photo.uri) }
                        )
                        Text(photo.name, color = Color.White, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { onShare(photo) }) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFB7DCE5))
                                Spacer(Modifier.width(6.dp))
                                Text("Share", color = Color(0xFFB7DCE5))
                            }
                            TextButton(onClick = { onDelete(photo) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF7777))
                                Spacer(Modifier.width(6.dp))
                                Text("Delete", color = Color(0xFFFF7777))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun refreshLocation(context: android.content.Context, onResult: (GpsInfo) -> Unit) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        request.addOnSuccessListener { location ->
            if (location == null) {
                client.lastLocation.addOnSuccessListener { cached ->
                    if (cached != null) resolveLocation(context, cached, onResult)
                    else onResult(GpsInfo(address = "Waiting for GPS fix"))
                }.addOnFailureListener { onResult(GpsInfo(address = "GPS unavailable")) }
            } else resolveLocation(context, location, onResult)
        }.addOnFailureListener {
            client.lastLocation.addOnSuccessListener { cached ->
                if (cached != null) resolveLocation(context, cached, onResult)
                else onResult(GpsInfo(address = "GPS unavailable"))
            }.addOnFailureListener { onResult(GpsInfo(address = "GPS unavailable")) }
        }
    } catch (_: SecurityException) { onResult(GpsInfo(address = "Location permission needed")) }
}

private fun resolveLocation(context: android.content.Context, location: Location, onResult: (GpsInfo) -> Unit) {
    val mock = if (Build.VERSION.SDK_INT >= 31) location.isMock else @Suppress("DEPRECATION") location.isFromMockProvider
    val ageSec = ((System.currentTimeMillis() - location.time).coerceAtLeast(0L) / 1000L)
    var info = GpsInfo(location.latitude, location.longitude, "Resolving address…", location.accuracy,
        location.time, if (location.hasAltitude()) location.altitude else null,
        if (location.hasSpeed()) location.speed else null,
        if (location.hasBearing()) location.bearing else null, mock)
    onResult(info.copy(address = if (ageSec > 120) "Stale fix · ${ageSec}s old" else "Resolving address…"))
    if (!Geocoder.isPresent()) { onResult(info.copy(address = "${info.coordinateText}")); return }
    val geocoder = Geocoder(context, Locale.getDefault())
    if (Build.VERSION.SDK_INT >= 33) {
        try {
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                val a = addresses.firstOrNull()
                val line = listOfNotNull(a?.thoroughfare, a?.subLocality, a?.locality, a?.adminArea).distinct().joinToString(", ")
                val address = line.ifBlank { "${info.coordinateText}" }
                // cache the resolved address for offline fallback
                context.getSharedPreferences("gps_address_cache", android.content.Context.MODE_PRIVATE)
                    .edit().putString("${location.latitude},${location.longitude}", address).putString("last_address", address).apply()
                onResult(info.copy(address = address))
            }
        } catch (_: Throwable) { onResult(info.copy(address = cachedAddress(context, info.coordinateText))) }
    } else {
        Executors.newSingleThreadExecutor().execute {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val a = addresses?.firstOrNull()
                val line = listOfNotNull(a?.thoroughfare, a?.subLocality, a?.locality, a?.adminArea).distinct().joinToString(", ")
                val address = line.ifBlank { info.coordinateText }
                context.getSharedPreferences("gps_address_cache", android.content.Context.MODE_PRIVATE)
                    .edit().putString("last_address", address).apply()
                onResult(info.copy(address = address))
            } catch (_: Throwable) { onResult(info.copy(address = cachedAddress(context, info.coordinateText))) }
        }
    }
}

private fun cachedAddress(context: android.content.Context, fallback: String): String =
    context.getSharedPreferences("gps_address_cache", android.content.Context.MODE_PRIVATE).getString("last_address", fallback) ?: fallback

private fun takePhoto(context: android.content.Context, imageCapture: ImageCapture?, gps: GpsInfo, mapSnapshot: Bitmap?, report: (String) -> Unit, onPhotoSaved: (Uri) -> Unit = {}) {
    if (imageCapture == null) { report("Camera is not ready"); return }
    val temp = File(context.cacheDir, "wiz_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(temp).build()
    imageCapture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onError(exception: ImageCaptureException) { report("Capture failed: ${exception.message}") }
        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
            Executors.newSingleThreadExecutor().execute {
            try {
                val bitmap = decodeExifOrientedBitmap(temp)
                val stamped = stampPhoto(bitmap, gps, mapSnapshot)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "WizGPS_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wiz GPS Camera")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        stamped.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    try {
                        context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                            val exif = ExifInterface(descriptor.fileDescriptor)
                            if (gps.lat != null && gps.lon != null) exif.setLatLong(gps.lat, gps.lon)
                            gps.fixTimeMillis?.let { exif.setAttribute(ExifInterface.TAG_DATETIME, SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(it))) }
                            exif.setAttribute("UserComment", "Wiz GPS Camera | ${gps.coordinateText} | ${gps.address}")
                            exif.saveAttributes()
                        }
                    } catch (_: Exception) {}
                    ContextCompat.getMainExecutor(context).execute { onPhotoSaved(uri); report("Photo saved · ready to share") }
                } else ContextCompat.getMainExecutor(context).execute { report("Unable to save photograph") }
                temp.delete()
            } catch (e: Exception) { ContextCompat.getMainExecutor(context).execute { report("Photo processing failed: ${e.localizedMessage}") } }
            }
        }
    })
}

private fun decodeExifOrientedBitmap(file: File): Bitmap {
    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        ?: throw IllegalStateException("Unable to decode captured image")
    val orientation = try { ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
    }
    return if (matrix.isIdentity()) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun stampPhoto(source: Bitmap, gps: GpsInfo, mapSnapshot: Bitmap?): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val scale = result.width / 1080f
    val pad = 28f * scale
    val boxWidth = result.width * .90f
    val boxHeight = 205f * scale
    val left = (result.width - boxWidth) / 2f
    val top = result.height - boxHeight - pad
    val rect = RectF(left, top, left + boxWidth, top + boxHeight)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD910171D.toInt() }
    canvas.drawRoundRect(rect, 20f * scale, 20f * scale, bg)

    val mapW = 210f * scale
    val mapRect = RectF(left + 10f * scale, top + 10f * scale, left + mapW, top + boxHeight - 10f * scale)
    if (mapSnapshot != null && !mapSnapshot.isRecycled) {
        val mapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(mapSnapshot, null, mapRect, mapPaint)
    } else {
        val fallback = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF263844.toInt() }
        canvas.drawRoundRect(mapRect, 10f * scale, 10f * scale, fallback)
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF5252.toInt(); textSize = 42f * scale; textAlign = Paint.Align.CENTER }
        canvas.drawText("●", mapRect.centerX(), mapRect.centerY(), pinPaint)
    }

    val textLeft = left + mapW + 24f * scale
    val maxTextWidth = rect.right - textLeft - 18f * scale
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 31f * scale; typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL) }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF1.toInt(); textSize = 23f * scale }
    val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF1.toInt(); textSize = 20f * scale }
    fun drawFit(text: String, y: Float, paint: Paint) {
        var shown = text
        while (shown.isNotEmpty() && paint.measureText(shown) > maxTextWidth) shown = shown.dropLast(1)
        if (shown != text && shown.length > 3) shown = shown.dropLast(3) + "..."
        canvas.drawText(shown, textLeft, y, paint)
    }
    val address = (gps.address + if (gps.isMock) " · MOCK LOCATION" else "").take(110)
    drawFit(address, top + 43f * scale, titlePaint)
    drawFit(gps.coordinateText, top + 78f * scale, bodyPaint)
    drawFit(SimpleDateFormat("EEEE, dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(Date()), top + 112f * scale, bodyPaint)
    val fixAge = gps.fixTimeMillis?.let { ((System.currentTimeMillis() - it).coerceAtLeast(0L) / 1000L) }
    drawFit("GPS fix ${fixAge?.let { "$it s old" } ?: "age unknown"} · ${gps.accuracy?.let { "±${it.toInt()} m" } ?: "accuracy unknown"}", top + 146f * scale, smallPaint)
    drawFit("Note: Captured by Wiz GPS Camera", top + 178f * scale, smallPaint)
    return result
}
