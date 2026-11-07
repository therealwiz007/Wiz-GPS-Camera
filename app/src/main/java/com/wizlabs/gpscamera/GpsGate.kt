package com.wizlabs.gpscamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

private fun hasPreciseLocation(context: Context): Boolean =
    isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)

private fun isGpsOn(context: Context): Boolean = try {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
} catch (e: Exception) {
    false
}

private fun openLocationSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // no settings screen available; nothing else to do
    }
}

private fun openAppSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        // ignore
    }
}

/**
 * The app does not open until precise location is allowed, GPS is switched on and a fresh GPS fix
 * has arrived. Once it is open it stays open, unless location permission is removed or GPS is
 * switched off, in which case this screen returns.
 */
@Composable
fun GpsGate(location: LocationEngine, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasPreciseLocation(context)) }
    var gpsOn by remember { mutableStateOf(isGpsOn(context)) }
    var opened by remember { mutableStateOf(false) }
    var waited by remember { mutableIntStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = hasPreciseLocation(context)
        if (hasPermission) location.start()
    }
    val resolveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        gpsOn = isGpsOn(context)
    }

    LaunchedEffect(Unit) {
        if (!hasPreciseLocation(context)) {
            permLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Re-check once a second: this also catches the user coming back from the Settings screen.
    LaunchedEffect(Unit) {
        while (true) {
            hasPermission = hasPreciseLocation(context)
            gpsOn = isGpsOn(context)
            if (hasPermission && gpsOn) location.start()
            waited++
            delay(1000)
        }
    }

    val fix = location.fix
    val fresh = fix != null && fix.ageSeconds() <= 30 && waited >= 0

    LaunchedEffect(hasPermission, gpsOn, fresh) {
        opened = if (!hasPermission || !gpsOn) false else (opened || fresh)
    }

    if (opened) {
        content()
        return
    }

    val turnOnGps: () -> Unit = {
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build())
            .setAlwaysShow(true)
            .build()
        LocationServices.getSettingsClient(context).checkLocationSettings(request)
            .addOnSuccessListener { gpsOn = isGpsOn(context) }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        resolveLauncher.launch(IntentSenderRequest.Builder(e.resolution).build())
                    } catch (ex: Exception) {
                        openLocationSettings(context)
                    }
                } else {
                    openLocationSettings(context)
                }
            }
    }

    val step = when {
        !hasPermission -> 0
        !gpsOn -> 1
        else -> 2
    }
    val requestPermission: () -> Unit = {
        permLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B2230), WizNavy, WizBar)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            RadarLogo(searching = step == 2)
            Spacer(Modifier.height(28.dp))
            Text(
                when (step) {
                    0 -> "Photos that prove\nwhere you were"
                    1 -> "Switch on GPS"
                    else -> "Locking on to satellites"
                },
                color = WizText,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when (step) {
                    0 -> "Every photo is stamped with the exact place, map and time. Allow precise location to start."
                    1 -> "Wiz GPS Camera needs GPS switched on to stamp your photos accurately."
                    else -> fix?.accuracyM?.let { "Current accuracy ±${it.toInt()} m — almost there." }
                        ?: "This usually takes a few seconds."
                },
                color = WizMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (step == 2 && waited >= 20) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Taking a while? Step near a window or outside for a clearer view of the sky.",
                    color = WizWarn,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            // Three-step progress so people know how close they are.
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WizPanel.copy(alpha = .85f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                StepRow(1, "Precise location allowed", done = step > 0, current = step == 0)
                StepRow(2, "GPS switched on", done = step > 1, current = step == 1)
                StepRow(3, "Fresh GPS fix", done = false, current = step == 2)
            }
            Spacer(Modifier.height(18.dp))

            when (step) {
                0 -> {
                    Button(
                        onClick = requestPermission,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) { Text("Allow location", style = MaterialTheme.typography.titleSmall) }
                    TextButton(onClick = { openAppSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open app settings", color = WizMuted)
                    }
                }
                1 -> {
                    Button(
                        onClick = turnOnGps,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) { Text("Turn on GPS", style = MaterialTheme.typography.titleSmall) }
                    TextButton(onClick = { openLocationSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open location settings", color = WizMuted)
                    }
                }
                else -> {
                    LinearProgressIndicator(
                        color = WizTeal,
                        trackColor = WizPanelHi,
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50))
                    )
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

/** App icon with expanding sonar rings while searching for a fix. */
@Composable
private fun RadarLogo(searching: Boolean) {
    val t = rememberInfiniteTransition(label = "radar")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "radarPhase"
    )
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxR = size.minDimension / 2f
            val ringCount = 3
            for (i in 0 until ringCount) {
                val p = (phase + i / ringCount.toFloat()) % 1f
                val r = maxR * (0.32f + 0.68f * p)
                val a = (1f - p) * if (searching) 0.55f else 0.22f
                drawCircle(WizTeal.copy(alpha = a), radius = r, style = Stroke(width = 2.dp.toPx()))
            }
            drawCircle(WizTeal.copy(alpha = .08f), radius = maxR * 0.34f)
        }
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(104.dp).clip(RoundedCornerShape(28.dp))
        )
    }
}

@Composable
private fun StepRow(number: Int, label: String, done: Boolean, current: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> WizGood
                        current -> WizTeal
                        else -> WizPanelHi
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = WizNavy, modifier = Modifier.size(17.dp))
            } else {
                Text(
                    number.toString(),
                    color = if (current) WizNavy else WizMuted,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (done || current) WizText else WizMuted,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
