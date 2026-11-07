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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
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
    "Pakistan" to "Asia/Karachi",
    "Bangladesh" to "Asia/Dhaka",
    "Nepal" to "Asia/Kathmandu",
    "Dubai" to "Asia/Dubai",
    "Riyadh" to "Asia/Riyadh",
    "Singapore" to "Asia/Singapore",
    "Hong Kong" to "Asia/Hong_Kong",
    "Tokyo" to "Asia/Tokyo",
    "Sydney" to "Australia/Sydney",
    "London" to "Europe/London",
    "Paris / Berlin" to "Europe/Berlin",
    "Johannesburg" to "Africa/Johannesburg",
    "Lagos" to "Africa/Lagos",
    "New York" to "America/New_York",
    "Chicago" to "America/Chicago",
    "Denver" to "America/Denver",
    "Los Angeles" to "America/Los_Angeles",
    "São Paulo" to "America/Sao_Paulo"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    repo: SettingsRepo,
    projects: ProjectStore,
    location: LocationEngine,
    sensors: SensorHub,
    onBack: () -> Unit
) {
    val s = repo.settings
    val context = LocalContext.current
    var logoPresent by remember(s.logoVersion) { mutableStateOf(LogoStore.exists(context)) }
    var newTag by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Project?>(null) }
    var deleting by remember { mutableStateOf<Project?>(null) }

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
            StampPreview(s, location, sensors, projects.byId(s.projectId)?.name ?: "")
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
                CardDivider()
                OptionGroup("Style", StampStyle.values().map { it.label to it }, s.style, Icons.Filled.Style) { v ->
                    repo.update { it.copy(style = v) }
                }
                CardDivider()
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Filled.Palette)
                        Spacer(Modifier.width(14.dp))
                        Text("Accent colour", color = WizText, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        STAMP_ACCENTS.forEach { (name, argb) ->
                            val picked = s.accent == argb
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(if (picked) 3.dp else 1.dp, if (picked) Color.White else WizLine, CircleShape)
                                    .clickable(onClickLabel = name) { repo.update { it.copy(accent = argb) } }
                                    .semantics { contentDescription = name; selected = picked },
                                contentAlignment = Alignment.Center
                            ) {
                                if (picked) Icon(Icons.Filled.Check, contentDescription = null, tint = WizNavy, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                CardDivider()
                LabeledSlider("Card opacity", Icons.Filled.Opacity, s.opacity, 30, 100) { v -> repo.update { it.copy(opacity = v) } }
                LabeledSlider("Text size", Icons.Filled.FormatSize, s.textScale, 80, 140) { v -> repo.update { it.copy(textScale = v) } }
            }

            SectionTitle("Units and coordinates", Icons.Filled.SquareFoot)
            SettingsCard {
                OptionGroup("Coordinate format", CoordFormat.values().map { it.label to it }, s.coordFormat, Icons.Filled.GridOn) { v ->
                    repo.update { it.copy(coordFormat = v) }
                }
                Text(
                    "Example: " + Geo.format(28.6129, 77.2295, s.coordFormat),
                    color = WizMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 48.dp, bottom = 6.dp)
                )
                CardDivider()
                OptionGroup("Units", Units.values().map { it.label to it }, s.units, Icons.Filled.Straighten) { v ->
                    repo.update { it.copy(units = v) }
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

            SectionTitle("Quick tags", Icons.Filled.Label)
            SettingsCard {
                Text(
                    "Tap a tag in the note dialog on the camera to add it to the note. Tap a tag here to remove it.",
                    color = WizMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    s.tags.forEach { tag ->
                        PillChip("$tag  ✕", false) { repo.update { it.copy(tags = it.tags - tag) } }
                    }
                }
                Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it.take(30) },
                        singleLine = true,
                        label = { Text("New tag") },
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(
                        enabled = newTag.isNotBlank() && newTag.trim() !in s.tags,
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            repo.update { it.copy(tags = it.tags + newTag.trim()) }
                            newTag = ""
                        }
                    ) { Text("Add") }
                }
            }

            SectionTitle("Projects", Icons.Filled.Folder)
            SettingsCard {
                if (projects.projects.isEmpty()) {
                    Text(
                        "No projects yet. Create one from the Project pill on the camera screen.",
                        color = WizMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                projects.projects.sortedByDescending { it.createdMs }.forEachIndexed { i, p ->
                    if (i > 0) CardDivider()
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Filled.Folder, if (p.id == s.projectId) WizTeal else WizMuted)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            p.name,
                            color = WizText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (p.id == s.projectId) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { renaming = p }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename ${p.name}", tint = WizMuted)
                        }
                        IconButton(onClick = { deleting = p }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${p.name}", tint = WizBad)
                        }
                    }
                }
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
                SwitchRow("Shutter sound", s.shutterSound, { v -> repo.update { it.copy(shutterSound = v) } }, icon = Icons.Filled.VolumeUp)
                SwitchRow(
                    "Volume keys take a photo", s.volumeShutter, { v -> repo.update { it.copy(volumeShutter = v) } },
                    icon = Icons.Filled.CameraAlt
                )
                SwitchRow(
                    "Mirror front-camera photos", s.mirrorFront, { v -> repo.update { it.copy(mirrorFront = v) } },
                    "Off saves selfies the way others see you", Icons.Filled.Flip
                )
                SwitchRow("Compass on the camera", s.showCompass, { v -> repo.update { it.copy(showCompass = v) } }, icon = Icons.Filled.Navigation)
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

            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = { repo.update { it.copy(onboardingDone = false) } },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, WizLine),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Filled.School, contentDescription = null, tint = WizTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Show the intro again", color = WizText)
            }
            Spacer(Modifier.height(20.dp))
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

    renaming?.let { p ->
        var name by remember(p.id) { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            containerColor = WizPanel,
            title = { Text("Rename project") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors
                )
            },
            confirmButton = {
                Button(enabled = name.isNotBlank(), shape = RoundedCornerShape(12.dp), onClick = {
                    projects.rename(p.id, name)
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel", color = WizMuted) } }
        )
    }

    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = WizPanel,
            title = { Text("Delete “${p.name}”?") },
            text = { Text("The project is removed from the list. Its photos stay in your gallery.") },
            confirmButton = {
                TextButton(onClick = {
                    projects.delete(p.id)
                    if (repo.settings.projectId == p.id) repo.update { it.copy(projectId = "") }
                    deleting = null
                }) { Text("Delete", color = WizBad) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel", color = WizMuted) } }
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon)
            Spacer(Modifier.width(14.dp))
            Text(label, color = WizText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("$value%", color = WizMuted, style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min) / 5 - 1,
            colors = SliderDefaults.colors(thumbColor = WizTeal, activeTrackColor = WizTeal, inactiveTrackColor = WizPanelHi)
        )
    }
}

/** A sample "photo" with the real stamp drawn on it, refreshed every second. */
@Composable
private fun StampPreview(s: AppSettings, location: LocationEngine, sensors: SensorHub, projectName: String) {
    val context = LocalContext.current
    val logo = remember(s.logoVersion) { LogoStore.load(context) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(s, logo, projectName) {
        val buffers = arrayOf(
            Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
        )
        var index = 0
        while (true) {
            val data = buildStampData(
                s, location.fix, location.address, location.addressCached,
                location.weather, sensors.headingDeg, location.declination, "WGC-PREVIEW",
                projectName
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
