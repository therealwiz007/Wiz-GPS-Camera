package com.wizlabs.gpscamera

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE_FORMATS = listOf("dd MMM yyyy", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd")

private val TIME_ZONES = listOf(
    "Device" to "",
    "UTC" to "UTC",
    "India" to "Asia/Kolkata",
    "Dubai" to "Asia/Dubai",
    "Singapore" to "Asia/Singapore",
    "London" to "Europe/London",
    "New York" to "America/New_York",
    "Sydney" to "Australia/Sydney"
)

@Composable
fun SettingsScreen(repo: SettingsRepo, location: LocationEngine, sensors: SensorHub, onBack: () -> Unit) {
    val s = repo.settings
    val context = LocalContext.current
    var logoPresent by remember(s.logoVersion) { mutableStateOf(LogoStore.exists(context)) }

    BackHandler(onBack = onBack)

    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            if (LogoStore.save(context, uri)) {
                repo.update { it.copy(logoVersion = it.logoVersion + 1, showLogo = true) }
            } else {
                Toast.makeText(context, "Could not use that image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = WizTeal,
        unfocusedBorderColor = WizLine,
        cursorColor = WizTeal,
        focusedLabelColor = WizTeal
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(WizNavy)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WizText)
            }
            Text("Settings", color = WizText, style = MaterialTheme.typography.titleLarge)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // ---- Live preview of the stamp --------------------------------------------------
            StampPreview(s, location, sensors)
            Text(
                "Live preview — every change below shows here instantly.",
                color = WizMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            // ---- Stamp ----------------------------------------------------------------------
            SectionTitle("Stamp", Icons.Filled.Layers)
            SettingsCard {
                OptionGroup("Layout", StampTemplate.values().map { it.label to it }, s.template, Icons.Filled.Layers) { v ->
                    repo.update { it.copy(template = v) }
                }
                CardDivider()
                OptionGroup("Position", StampPosition.values().map { it.label to it }, s.position, Icons.Filled.VerticalAlignBottom) { v ->
                    repo.update { it.copy(position = v) }
                }
            }

            SectionTitle("What the stamp shows", Icons.Filled.Place)
            SettingsCard {
                SwitchRow("Map", s.showMap, { v -> repo.update { it.copy(showMap = v) } }, "Only in the “Full with map” layout", Icons.Filled.Map)
                SwitchRow("Address", s.showAddress, { v -> repo.update { it.copy(showAddress = v) } }, icon = Icons.Filled.Place)
                SwitchRow("Coordinates", s.showCoords, { v -> repo.update { it.copy(showCoords = v) } }, "Always shown if the address can’t be found", Icons.Filled.MyLocation)
                SwitchRow("Date and time", s.showDateTime, { v -> repo.update { it.copy(showDateTime = v) } }, icon = Icons.Filled.Schedule)
                SwitchRow("GPS accuracy", s.showAccuracy, { v -> repo.update { it.copy(showAccuracy = v) } }, icon = Icons.Filled.GpsFixed)
                SwitchRow("Altitude", s.showAltitude, { v -> repo.update { it.copy(showAltitude = v) } }, icon = Icons.Filled.Terrain)
                SwitchRow("Compass heading", s.showHeading, { v -> repo.update { it.copy(showHeading = v) } }, icon = Icons.Filled.Explore)
                SwitchRow("Speed", s.showSpeed, { v -> repo.update { it.copy(showSpeed = v) } }, icon = Icons.Filled.Speed)
                SwitchRow("Plus code", s.showPlusCode, { v -> repo.update { it.copy(showPlusCode = v) } }, icon = Icons.Filled.Tag)
                SwitchRow(
                    "Weather", s.showWeather, { v -> repo.update { it.copy(showWeather = v) } },
                    "Temperature from Open-Meteo for your approximate location", Icons.Filled.Cloud
                )
                SwitchRow("Note", s.showNote, { v -> repo.update { it.copy(showNote = v) } }, icon = Icons.Filled.Edit)
                SwitchRow("Photo ID and time check", s.showVerification, { v -> repo.update { it.copy(showVerification = v) } }, icon = Icons.Filled.Fingerprint)
                SwitchRow("Logo", s.showLogo, { v -> repo.update { it.copy(showLogo = v) } }, icon = Icons.Filled.Image)
            }

            SectionTitle("Note", Icons.Filled.Edit)
            SettingsCard {
                OutlinedTextField(
                    value = s.note,
                    onValueChange = { v -> repo.update { it.copy(note = v) } },
                    label = { Text("Site name, job number, “Inspection – Pump 2”…") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            // ---- Date & time ----------------------------------------------------------------
            SectionTitle("Date and time", Icons.Filled.Schedule)
            SettingsCard {
                val now = Date()
                OptionGroup(
                    "Date format",
                    DATE_FORMATS.map { fmt -> SimpleDateFormat(fmt, Locale.getDefault()).format(now) to fmt },
                    s.dateFormat,
                    Icons.Filled.CalendarToday
                ) { v -> repo.update { it.copy(dateFormat = v) } }
                CardDivider()
                OptionGroup("Clock", ClockFormat.values().map { it.label to it }, s.clockFormat, Icons.Filled.Schedule) { v ->
                    repo.update { it.copy(clockFormat = v) }
                }
                CardDivider()
                OptionGroup("Time zone", TIME_ZONES, s.timeZoneId, Icons.Filled.Public) { v ->
                    repo.update { it.copy(timeZoneId = v) }
                }
                CardDivider()
                SwitchRow("Show seconds", s.showSeconds, { v -> repo.update { it.copy(showSeconds = v) } })
                SwitchRow("Show time zone (GMT offset)", s.showTimeZone, { v -> repo.update { it.copy(showTimeZone = v) } })
            }

            // ---- Branding -------------------------------------------------------------------
            SectionTitle("Your brand", Icons.Filled.Palette)
            SettingsCard {
                Row(
                    Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { pickLogo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (logoPresent) "Change logo" else "Choose logo") }
                    if (logoPresent) {
                        OutlinedButton(
                            onClick = {
                                LogoStore.remove(context)
                                logoPresent = false
                                repo.update { it.copy(logoVersion = it.logoVersion + 1) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, WizLine)
                        ) { Text("Remove", color = WizText) }
                    }
                }
                OutlinedTextField(
                    value = s.watermark,
                    onValueChange = { v -> repo.update { it.copy(watermark = v) } },
                    label = { Text("Watermark text (e.g. © Your name)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )
            }

            // ---- Camera ---------------------------------------------------------------------
            SectionTitle("Camera", Icons.Filled.CameraAlt)
            SettingsCard {
                OptionGroup("Aspect ratio", AspectChoice.values().map { it.label to it }, s.aspect, Icons.Filled.AspectRatio) { v ->
                    repo.update { it.copy(aspect = v) }
                }
                CardDivider()
                OptionGroup(
                    "Photo size",
                    listOf("5 MP" to 5, "8 MP" to 8, "12 MP" to 12, "16 MP" to 16),
                    s.megapixels,
                    Icons.Filled.HighQuality
                ) { v -> repo.update { it.copy(megapixels = v) } }
                CardDivider()
                OptionGroup("Self-timer", listOf("Off" to 0, "3 s" to 3, "10 s" to 10), s.timerSeconds, Icons.Filled.Timer) { v ->
                    repo.update { it.copy(timerSeconds = v) }
                }
                CardDivider()
                OptionGroup("Burst", listOf("Single" to 1, "3 photos" to 3, "5 photos" to 5), s.burstCount, Icons.Filled.BurstMode) { v ->
                    repo.update { it.copy(burstCount = v) }
                }
                CardDivider()
                SwitchRow("Vibrate on capture", s.vibrate, { v -> repo.update { it.copy(vibrate = v) } }, icon = Icons.Filled.Vibration)
                SwitchRow("Grid lines", s.gridOn, { v -> repo.update { it.copy(gridOn = v) } }, icon = Icons.Filled.Grid3x3)
                SwitchRow("Level indicator", s.levelOn, { v -> repo.update { it.copy(levelOn = v) } }, icon = Icons.Filled.Straighten)
            }

            // ---- Trust ----------------------------------------------------------------------
            SectionTitle("Trust and proof", Icons.Filled.Shield)
            SettingsCard {
                OptionGroup(
                    "If a fake (mock) location is detected",
                    MockPolicy.values().map { it.label to it },
                    s.mockPolicy,
                    Icons.Filled.Warning
                ) { v -> repo.update { it.copy(mockPolicy = v) } }
                Text(
                    "Photos are stamped with network-verified time when the phone has been online, and a " +
                        "warning is printed if the phone’s clock is wrong. Each saved photo gets an integrity " +
                        "record on this device; the gallery marks edited files. This can only be checked " +
                        "inside this app, on this phone.",
                    color = WizMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(Modifier.height(28.dp))
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
            }
            Text(
                "Wiz GPS Camera $version · Wiz Labs",
                color = WizMuted.copy(alpha = .7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** A sample "photo" with the real stamp drawn on it, refreshed every second. */
@Composable
private fun StampPreview(s: AppSettings, location: LocationEngine, sensors: SensorHub) {
    val context = LocalContext.current
    val logo = remember(s.logoVersion) { LogoStore.load(context) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(s, logo) {
        val buffers = arrayOf(
            Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
        )
        var index = 0
        while (true) {
            val data = buildStampData(
                s, location.fix, location.address, location.addressCached,
                location.weather, sensors.headingDeg, location.declination, "WGC-PREVIEW"
            )
            val target = buffers[index]
            index = 1 - index
            withContext(Dispatchers.Default) {
                target.eraseColor(0)
                StampRenderer.draw(android.graphics.Canvas(target), target.width, target.height, data, s, null, logo)
            }
            bitmap = target
            delay(1000)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF7CC6E0), Color(0xFF3E8DB0), Color(0xFF1F4E63))))
    ) {
        // A simple landscape so the preview reads as "a photo".
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x55FFF4C2), radius = size.minDimension * .10f, center = Offset(size.width * .78f, size.height * .22f))
            val far = Path().apply {
                moveTo(0f, size.height * .62f)
                lineTo(size.width * .22f, size.height * .42f)
                lineTo(size.width * .40f, size.height * .58f)
                lineTo(size.width * .62f, size.height * .36f)
                lineTo(size.width, size.height * .60f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(far, Color(0xFF2B6A80))
            val near = Path().apply {
                moveTo(0f, size.height * .78f)
                lineTo(size.width * .35f, size.height * .64f)
                lineTo(size.width * .70f, size.height * .76f)
                lineTo(size.width, size.height * .68f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(near, Color(0xFF173E4E))
        }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Stamp preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}
