@file:OptIn(ExperimentalLayoutApi::class)

package com.wizlabs.gpscamera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val WizNavy = Color(0xFF091923)
val WizTeal = Color(0xFF70DCE8)
val WizPanel = Color(0xFF132630)
val WizBar = Color(0xFF08131B)

@Composable
fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = WizTeal,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WizPanel)
            .padding(14.dp)
    ) { content() }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    // The whole row toggles, not just the switch.
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White)
            if (hint != null) {
                Text(hint, color = Color.White.copy(alpha = .6f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun <T> ChipRow(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        options.forEach { (label, value) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WizTeal.copy(alpha = .9f),
                    selectedLabelColor = WizNavy
                )
            )
        }
    }
}

/** Small round toggle used on the camera screen. */
@Composable
fun QuickToggle(icon: ImageVector, label: String?, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (active) WizTeal.copy(alpha = .92f) else Color(0x99091923),
        contentColor = if (active) WizNavy else Color.White
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            if (label != null) {
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
