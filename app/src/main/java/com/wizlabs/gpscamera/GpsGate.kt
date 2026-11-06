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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        Modifier
            .fillMaxSize()
            .background(WizBar)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(22.dp))
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "GPS is required",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))

        when {
            !hasPermission -> {
                Text(
                    "Allow precise location so every photo can be stamped with exactly where it was taken.",
                    color = Color(0xFFB7C2CC),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        permLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Allow location") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { openAppSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open app settings")
                }
            }
            !gpsOn -> {
                Text(
                    "Turn on GPS (location) to open the camera.",
                    color = Color(0xFFB7C2CC),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = turnOnGps, modifier = Modifier.fillMaxWidth()) { Text("Turn on GPS") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { openLocationSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open location settings")
                }
            }
            else -> {
                CircularProgressIndicator(color = WizTeal)
                Spacer(Modifier.height(16.dp))
                Text("Getting your GPS fix…", color = Color.White, textAlign = TextAlign.Center)
                val acc = fix?.accuracyM
                if (acc != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Current accuracy ±${acc.toInt()} m",
                        color = Color(0xFFB7C2CC),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (waited >= 20) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Taking a while? Move near a window or outside for a better signal.",
                        color = Color(0xFFB7C2CC),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
