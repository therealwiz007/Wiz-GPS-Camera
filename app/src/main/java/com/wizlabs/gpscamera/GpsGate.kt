package com.wizlabs.gpscamera

import androidx.compose.animation.Crossfade
import com.wizlabs.gpscamera.engine.Action
import com.wizlabs.gpscamera.engine.Expression
import com.wizlabs.gpscamera.engine.IrisBrain
import com.wizlabs.gpscamera.engine.IrisEvent
import com.wizlabs.gpscamera.ui.mascot.Iris

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

    // A fresh fix shows Iris "locked" for a moment, then the gate fades to the camera (320 ms).
    var lockedShown by remember { mutableStateOf(false) }
    LaunchedEffect(hasPermission, gpsOn, fresh) {
        if (!hasPermission || !gpsOn) {
            opened = false
            lockedShown = false
        } else if (!opened && fresh) {
            lockedShown = true
            delay(380)
            opened = true
        }
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

    Crossfade(targetState = opened, animationSpec = tween(320), label = "gate") { isOpen ->
        if (isOpen) {
            content()
        } else {
            GateScreen(
                step = step,
                accuracyM = fix?.accuracyM?.toInt(),
                waited = waited,
                locked = lockedShown,
                onAllow = requestPermission,
                onAppSettings = { openAppSettings(context) },
                onGps = turnOnGps,
                onLocationSettings = { openLocationSettings(context) }
            )
        }
    }
}

@Composable
private fun GateScreen(
    step: Int,
    accuracyM: Int?,
    waited: Int,
    locked: Boolean,
    onAllow: () -> Unit,
    onAppSettings: () -> Unit,
    onGps: () -> Unit,
    onLocationSettings: () -> Unit
) {
    val iris = when {
        locked -> IrisBrain.react(IrisEvent.FixLocked(accuracyM ?: 0))
        step == 0 -> IrisBrain.react(IrisEvent.WaitingPermission)
        step == 1 -> IrisBrain.react(IrisEvent.GpsOff)
        else -> IrisBrain.react(IrisEvent.SearchingGps(accuracyM, waited))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GpsSurface, GpsBg, GpsBg)))
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
            SatelliteLockHero(searching = step == 2 && !locked, modifier = Modifier.enterRise(0)) {
                Iris(iris.expression, iris.action, 120.dp)
            }
            Spacer(Modifier.height(24.dp))
            AnimatedContent(
                targetState = if (locked) 3 else step,
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(320)) { it / 4 }) togetherWith fadeOut(tween(150))
                },
                label = "gateTitle",
                modifier = Modifier.enterRise(1)
            ) { s ->
                Text(
                    when (s) {
                        0 -> "Photos that prove\nwhere you were"
                        1 -> "Switch on GPS"
                        2 -> "Getting a fix"
                        else -> "Locked"
                    },
                    color = GpsSilver,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    locked -> iris.line ?: ""
                    step == 0 -> "Every photo is stamped with the exact place, map and time. " + (iris.line ?: "")
                    step == 1 -> iris.line ?: ""
                    else -> iris.line ?: ""
                },
                color = GpsMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (step == 2 && !locked && waited >= 20) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Taking a while? Step near a window or outside for a clearer view of the sky.",
                    color = GpsAmber,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            // Three steps, so people know how close they are. Step 3 carries the live accuracy meter.
            Column(
                Modifier
                    .fillMaxWidth()
                    .enterRise(3)
                    .clip(RoundedCornerShape(22.dp))
                    .background(GpsSurface.copy(alpha = .9f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                StepRow(1, "Precise location", done = step > 0, current = step == 0)
                StepRow(2, "GPS on", done = step > 1, current = step == 1)
                StepRow(3, "Getting a fix", done = locked, current = step == 2 && !locked)
                if (step == 2 && !locked) AccuracyMeter(accuracyM)
            }
            Spacer(Modifier.height(18.dp))

            when (step) {
                0 -> {
                    Button(
                        onClick = onAllow,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GpsTeal, contentColor = GpsOnTeal),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) { Text("Allow location", style = MaterialTheme.typography.titleSmall) }
                    TextButton(onClick = onAppSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Open app settings", color = GpsMuted)
                    }
                }
                1 -> {
                    Button(
                        onClick = onGps,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GpsTeal, contentColor = GpsOnTeal),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) { Text("Turn on GPS", style = MaterialTheme.typography.titleSmall) }
                    TextButton(onClick = onLocationSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Open location settings", color = GpsMuted)
                    }
                }
                else -> Spacer(Modifier.height(54.dp))
            }
        }
    }
}

/** Live accuracy: a bar that fills as the fix tightens, with the number in mono. */
@Composable
private fun AccuracyMeter(accuracyM: Int?) {
    val progress = if (accuracyM == null) 0.06f else (1f - (accuracyM - 5f) / 55f).coerceIn(0.08f, 1f)
    val shown by androidx.compose.animation.core.animateFloatAsState(progress, tween(400), label = "acc")
    Column(Modifier.fillMaxWidth().padding(start = 42.dp, bottom = 12.dp, end = 4.dp)) {
        Text(
            if (accuracyM != null) "±$accuracyM m" else "Waiting for satellites",
            color = GpsTeal,
            style = MonoBody,
            modifier = Modifier.semantics { contentDescription = if (accuracyM != null) "Accuracy $accuracyM metres" else "Waiting for satellites" }
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(GpsSurfaceHi)) {
            Box(Modifier.fillMaxWidth(shown).height(5.dp).clip(RoundedCornerShape(50)).background(GpsTeal))
        }
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
                        done -> GpsGood
                        current -> GpsTeal
                        else -> GpsSurfaceHi
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = GpsBg, modifier = Modifier.size(17.dp))
            } else {
                Text(
                    number.toString(),
                    color = if (current) GpsBg else GpsMuted,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (done || current) GpsSilver else GpsMuted,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
