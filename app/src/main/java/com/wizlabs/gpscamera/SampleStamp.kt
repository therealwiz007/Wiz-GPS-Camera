package com.wizlabs.gpscamera

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A landscape "photo" drawn in code (no bitmap asset), used behind sample stamps. */
@Composable
fun SampleScene(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF7C46C), Color(0xFFD9774A), Color(0xFF4A2632))))) {
        drawCircle(Color(0x55FFF4C2), radius = size.minDimension * .10f, center = Offset(size.width * .78f, size.height * .22f))
        val far = Path().apply {
            moveTo(0f, size.height * .62f); lineTo(size.width * .22f, size.height * .42f); lineTo(size.width * .40f, size.height * .58f)
            lineTo(size.width * .62f, size.height * .36f); lineTo(size.width, size.height * .60f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(far, Color(0xFF8A4448))
        val near = Path().apply {
            moveTo(0f, size.height * .78f); lineTo(size.width * .35f, size.height * .64f); lineTo(size.width * .70f, size.height * .76f)
            lineTo(size.width, size.height * .68f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(near, Color(0xFF3A2029))
    }
}

/** Demo data for onboarding and store art. A made-up public place, not the user's. */
fun demoStampData(): StampData = StampData(
    lat = 12.97565, lon = 77.60712, accuracyM = 4f, fixAgeS = 2, altitudeM = 913.0, speedKmh = 0f,
    headingDeg = 42f, headingTrue = true,
    address = "MG Road, Bengaluru, Karnataka 560001, India", addressCached = false, weather = null,
    plusCode = PlusCode.encode(12.97565, 77.60712),
    timeMs = 1_764_146_400_000L, timeStatus = TimeSync.Status.NETWORK, clockDeviationMs = 0L,
    mock = false, note = "", photoId = "WGC-7K2Q9D", projectName = "Site visit"
)

/** The sample scene with the real [StampRenderer] stamp on it (rendered once, off the main thread). */
@Composable
fun SampleStampPhoto(modifier: Modifier = Modifier, corner: Dp = 22.dp) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        bitmap = withContext(Dispatchers.Default) {
            val b = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
            StampRenderer.draw(android.graphics.Canvas(b), b.width, b.height, demoStampData(), AppSettings(), null, null)
            b
        }
    }
    Box(modifier.fillMaxWidth().aspectRatio(3f / 2f).clip(RoundedCornerShape(corner))) {
        SampleScene()
        bitmap?.let {
            Image(it.asImageBitmap(), contentDescription = "Example of a stamped photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
    }
}
