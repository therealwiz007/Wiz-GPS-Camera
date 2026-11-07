package com.wizlabs.gpscamera

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.InputChip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SettingsScreen(repo: SettingsRepo, projects: ProjectStore, onBack: () -> Unit) {
    val s = repo.settings
    val context = LocalContext.current
    var logoPresent by remember(s.logoVersion) { mutableStateOf(LogoStore.exists(context)) }
    var newTag by remember { mutableStateOf("") }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
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

    Column(
        Modifier
            .fillMaxSize()
            .background(WizNavy)
            .statusBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Settings", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // ---- Stamp ----------------------------------------------------------------------
            SectionTitle("Stamp layout")
            SettingsCard {
                Text("Template", color = Color.White)
                ChipRow(StampTemplate.values().map { it.label to it }, s.template) { v ->
                    repo.update { it.copy(template = v) }
                }
                Spacer(Modifier.height(6.dp))
                Text("Position", color = Color.White)
                ChipRow(StampPosition.values().map { it.label to it }, s.position) { v ->
                    repo.update { it.copy(position = v) }
                }
            }

            SectionTitle("Stamp style")
            SettingsCard {
                ChipRow(StampStyle.values().map { it.label to it }, s.style) { v -> repo.update { it.copy(style = v) } }
                Spacer(Modifier.height(8.dp))
                Text("Accent colour", color = Color.White)
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    STAMP_ACCENTS.forEach { (name, argb) ->
                        val selected = s.accent == argb
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = .3f), CircleShape)
                                .clickable(onClickLabel = name) { repo.update { it.copy(accent = argb) } }
                                .semantics { contentDescription = name; this.selected = selected },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = WizNavy, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                LabeledSlider("Card opacity", s.opacity, 30, 100, "%") { v -> repo.update { it.copy(opacity = v) } }
                LabeledSlider("Text size", s.textScale, 80, 140, "%") { v -> repo.update { it.copy(textScale = v) } }
            }

            SectionTitle("Units and coordinates")
            SettingsCard {
                Text("Coordinate format", color = Color.White)
                ChipRow(CoordFormat.values().map { it.label to it }, s.coordFormat) { v -> repo.update { it.copy(coordFormat = v) } }
                Text(
                    Geo.format(28.6129, 77.2295, s.coordFormat),
                    color = Color.White.copy(alpha = .6f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(6.dp))
                Text("Units", color = Color.White)
                ChipRow(Units.values().map { it.label to it }, s.units) { v -> repo.update { it.copy(units = v) } }
            }

            SectionTitle("Fields on the stamp")
            SettingsCard {
                SwitchRow("Map", s.showMap, { v -> repo.update { it.copy(showMap = v) } }, "Only in the “Full with map” template")
                SwitchRow("Address", s.showAddress, { v -> repo.update { it.copy(showAddress = v) } })
                SwitchRow("Coordinates", s.showCoords, { v -> repo.update { it.copy(showCoords = v) } }, "Always shown if the address can’t be found")
                SwitchRow("Date and time", s.showDateTime, { v -> repo.update { it.copy(showDateTime = v) } })
                SwitchRow("GPS accuracy", s.showAccuracy, { v -> repo.update { it.copy(showAccuracy = v) } })
                SwitchRow("Altitude", s.showAltitude, { v -> repo.update { it.copy(showAltitude = v) } })
                SwitchRow("Compass heading", s.showHeading, { v -> repo.update { it.copy(showHeading = v) } })
                SwitchRow("Speed", s.showSpeed, { v -> repo.update { it.copy(showSpeed = v) } })
                SwitchRow("Plus code", s.showPlusCode, { v -> repo.update { it.copy(showPlusCode = v) } })
                SwitchRow(
                    "Weather", s.showWeather, { v -> repo.update { it.copy(showWeather = v) } },
                    "Fetches temperature from Open-Meteo using your approximate location"
                )
                SwitchRow("Note", s.showNote, { v -> repo.update { it.copy(showNote = v) } })
                SwitchRow("Photo ID and time check line", s.showVerification, { v -> repo.update { it.copy(showVerification = v) } })
                SwitchRow("Logo", s.showLogo, { v -> repo.update { it.copy(showLogo = v) } })
            }

            SectionTitle("Note")
            SettingsCard {
                OutlinedTextField(
                    value = s.note,
                    onValueChange = { v -> repo.update { it.copy(note = v) } },
                    label = { Text("Site name, job number, “Inspection – Pump 2”…") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionTitle("Quick tags")
            SettingsCard {
                Text(
                    "Tap a tag in the note dialog on the camera screen to add it to the note.",
                    color = Color.White.copy(alpha = .6f),
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    s.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { repo.update { it.copy(tags = it.tags - tag) } },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $tag", modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it.take(30) },
                        singleLine = true,
                        label = { Text("New tag") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.size(8.dp))
                    Button(
                        enabled = newTag.isNotBlank() && newTag.trim() !in s.tags,
                        onClick = {
                            repo.update { it.copy(tags = it.tags + newTag.trim()) }
                            newTag = ""
                        }
                    ) { Text("Add") }
                }
            }

            SectionTitle("Projects")
            SettingsCard {
                if (projects.projects.isEmpty()) {
                    Text(
                        "No projects yet. Create one from the folder chip on the camera screen.",
                        color = Color.White.copy(alpha = .6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                projects.projects.sortedByDescending { it.createdMs }.forEach { p ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Folder, contentDescription = null, tint = if (p.id == s.projectId) WizTeal else Color.White.copy(alpha = .7f))
                        Spacer(Modifier.size(10.dp))
                        Text(
                            p.name,
                            color = Color.White,
                            fontWeight = if (p.id == s.projectId) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { renaming = p }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = Color.White)
                        }
                        IconButton(onClick = { deleting = p }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                }
            }

            // ---- Date & time ----------------------------------------------------------------
            SectionTitle("Date and time")
            SettingsCard {
                Text("Date format", color = Color.White)
                val now = Date()
                ChipRow(
                    DATE_FORMATS.map { fmt -> SimpleDateFormat(fmt, Locale.getDefault()).format(now) to fmt },
                    s.dateFormat
                ) { v -> repo.update { it.copy(dateFormat = v) } }
                Spacer(Modifier.height(6.dp))
                Text("Clock", color = Color.White)
                ChipRow(ClockFormat.values().map { it.label to it }, s.clockFormat) { v ->
                    repo.update { it.copy(clockFormat = v) }
                }
                Spacer(Modifier.height(6.dp))
                Text("Time zone", color = Color.White)
                ChipRow(TIME_ZONES, s.timeZoneId) { v -> repo.update { it.copy(timeZoneId = v) } }
                SwitchRow("Show seconds", s.showSeconds, { v -> repo.update { it.copy(showSeconds = v) } })
                SwitchRow("Show time zone (GMT offset)", s.showTimeZone, { v -> repo.update { it.copy(showTimeZone = v) } })
            }

            // ---- Branding -------------------------------------------------------------------
            SectionTitle("Logo and watermark")
            SettingsCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        pickLogo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (logoPresent) "Change logo" else "Choose logo") }
                    if (logoPresent) {
                        OutlinedButton(onClick = {
                            LogoStore.remove(context)
                            logoPresent = false
                            repo.update { it.copy(logoVersion = it.logoVersion + 1) }
                        }) { Text("Remove") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = s.watermark,
                    onValueChange = { v -> repo.update { it.copy(watermark = v) } },
                    label = { Text("Watermark text (e.g. © Your name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---- Camera ---------------------------------------------------------------------
            SectionTitle("Camera")
            SettingsCard {
                Text("Aspect ratio", color = Color.White)
                ChipRow(AspectChoice.values().map { it.label to it }, s.aspect) { v ->
                    repo.update { it.copy(aspect = v) }
                }
                Spacer(Modifier.height(6.dp))
                Text("Photo size", color = Color.White)
                ChipRow(listOf("5 MP" to 5, "8 MP" to 8, "12 MP" to 12, "16 MP" to 16), s.megapixels) { v ->
                    repo.update { it.copy(megapixels = v) }
                }
                Spacer(Modifier.height(6.dp))
                Text("Timer", color = Color.White)
                ChipRow(listOf("Off" to 0, "3 s" to 3, "10 s" to 10), s.timerSeconds) { v ->
                    repo.update { it.copy(timerSeconds = v) }
                }
                Spacer(Modifier.height(6.dp))
                Text("Burst", color = Color.White)
                ChipRow(listOf("Single" to 1, "3 photos" to 3, "5 photos" to 5), s.burstCount) { v ->
                    repo.update { it.copy(burstCount = v) }
                }
                SwitchRow("Vibrate when a photo is taken", s.vibrate, { v -> repo.update { it.copy(vibrate = v) } })
                SwitchRow("Shutter sound", s.shutterSound, { v -> repo.update { it.copy(shutterSound = v) } })
                SwitchRow("Volume keys take a photo", s.volumeShutter, { v -> repo.update { it.copy(volumeShutter = v) } })
                SwitchRow(
                    "Mirror front-camera photos", s.mirrorFront, { v -> repo.update { it.copy(mirrorFront = v) } },
                    "Off saves selfies the way others see you"
                )
                SwitchRow("Compass on the preview", s.showCompass, { v -> repo.update { it.copy(showCompass = v) } })
                SwitchRow("Grid lines", s.gridOn, { v -> repo.update { it.copy(gridOn = v) } })
                SwitchRow("Level indicator", s.levelOn, { v -> repo.update { it.copy(levelOn = v) } })
            }

            // ---- Trust ----------------------------------------------------------------------
            SectionTitle("Trust and proof")
            SettingsCard {
                Text("If a mock/fake location is detected", color = Color.White)
                ChipRow(MockPolicy.values().map { it.label to it }, s.mockPolicy) { v ->
                    repo.update { it.copy(mockPolicy = v) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Photos are stamped with network-verified time when the phone has been online, and a " +
                        "warning is printed if the phone’s clock is wrong. Each saved photo gets an integrity " +
                        "record on this device; the gallery marks edited files. This can only be checked " +
                        "inside this app, on this phone.",
                    color = Color.White.copy(alpha = .65f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { repo.update { it.copy(onboardingDone = false) } },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Show the intro again") }
            Spacer(Modifier.height(16.dp))
            Text(
                "Wiz GPS Camera $version · Wiz Labs",
                color = Color.White.copy(alpha = .45f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    renaming?.let { p ->
        var name by remember(p.id) { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename project") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    projects.rename(p.id, name)
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }

    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${p.name}”?") },
            text = { Text("The project is removed from the list. Its photos stay in your gallery.") },
            confirmButton = {
                TextButton(onClick = {
                    projects.delete(p.id)
                    if (repo.settings.projectId == p.id) repo.update { it.copy(projectId = "") }
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LabeledSlider(label: String, value: Int, min: Int, max: Int, suffix: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Text("$value$suffix", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelMedium)
    }
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        valueRange = min.toFloat()..max.toFloat(),
        steps = (max - min) / 5 - 1,
        colors = SliderDefaults.colors(thumbColor = WizTeal, activeTrackColor = WizTeal)
    )
}
