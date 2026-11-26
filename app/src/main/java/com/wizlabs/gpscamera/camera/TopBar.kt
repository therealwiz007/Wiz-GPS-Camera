package com.wizlabs.gpscamera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Top glass bar (56 dp): project pill on the left; level, note, stamp style and settings on the right.
 * Same actions as before the redesign, only regrouped. Sits above the preview frame.
 */
@Composable
fun CameraTopBar(
    projectName: String?,
    levelOn: Boolean,
    noteSet: Boolean,
    templateLabel: String,
    onProject: () -> Unit,
    onLevel: () -> Unit,
    onNote: () -> Unit,
    onStamp: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(GpsGlass, RoundedCornerShape(22.dp))
            .border(1.dp, GpsLine, RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPill(
            text = projectName ?: "Project",
            icon = Icons.Filled.Folder,
            active = projectName != null,
            onClick = onProject,
            modifier = Modifier.widthIn(max = 136.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Filled.Straighten, "Level", onLevel, active = levelOn, size = 44.dp)
            GlassIconButton(Icons.Filled.Edit, "Note", onNote, active = noteSet, size = 44.dp)
            GlassIconButton(Icons.Filled.Layers, "Stamp style, $templateLabel", onStamp, size = 44.dp)
            GlassIconButton(Icons.Filled.Settings, "Settings", onSettings, size = 44.dp)
        }
    }
}
