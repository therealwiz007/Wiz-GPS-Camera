package com.wizlabs.gpscamera

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
fun SettingsScreen(repo: SettingsRepo, onBack: () -> Unit) {
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
                ChipRow(listOf("5 MP" to 5, "8 MP" to 8, "12 MP" to 12), s.megapixels) { v ->
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

            Spacer(Modifier.height(24.dp))
            Text(
                "Wiz GPS Camera 1.1.0 · Wiz Labs",
                color = Color.White.copy(alpha = .45f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
