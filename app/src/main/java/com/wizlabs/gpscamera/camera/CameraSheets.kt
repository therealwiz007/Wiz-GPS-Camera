@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.wizlabs.gpscamera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import android.media.MediaActionSound
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ProjectPickerDialog(
    projects: ProjectStore,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "File new photos under",
    allowNone: Boolean = true
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GpsSurface,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Photos are saved in a folder named after the project, and its name is printed on the stamp.",
                    color = GpsMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                if (allowNone) ProjectRow("No project", selectedId.isEmpty()) { onSelect("") }
                projects.projects.sortedByDescending { it.createdMs }.forEach { p ->
                    ProjectRow(p.name, p.id == selectedId) { onSelect(p.id) }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(60) },
                        singleLine = true,
                        placeholder = { Text("New project") },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GpsTeal,
                            unfocusedBorderColor = GpsLine,
                            cursorColor = GpsTeal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Create project",
                        enabled = newName.isNotBlank(),
                        active = newName.isNotBlank(),
                        onClick = { onSelect(projects.create(newName).id) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = GpsMuted) } }
    )
}

@Composable
private fun ProjectRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val tick = rememberTick()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) GpsTeal.copy(alpha = .16f) else Color.Transparent)
            .clickable { tick(); onClick() }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, tint = if (selected) GpsTeal else GpsMuted)
        Spacer(Modifier.width(12.dp))
        Text(name, color = GpsSilver, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ---- Pieces ---------------------------------------------------------------------------------

@Composable
internal fun StampSheet(settings: AppSettings, repo: SettingsRepo, onMore: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 22.dp, end = 22.dp, bottom = 20.dp)
    ) {
        Text("Stamp style", style = MaterialTheme.typography.titleLarge, color = GpsSilver)
        Text("Changes appear live on the camera.", style = MaterialTheme.typography.bodySmall, color = GpsMuted)
        Spacer(Modifier.height(8.dp))
        OptionGroup(
            label = "Layout",
            icon = Icons.Filled.Layers,
            options = StampTemplate.values().map { it.label to it },
            selected = settings.template
        ) { v -> repo.update { it.copy(template = v) } }
        OptionGroup(
            label = "Position",
            icon = Icons.Filled.VerticalAlignBottom,
            options = StampPosition.values().map { it.label to it },
            selected = settings.position
        ) { v -> repo.update { it.copy(position = v) } }
        OptionGroup(
            label = "Style",
            icon = Icons.Filled.Palette,
            options = StampStyle.values().map { it.label to it },
            selected = settings.style
        ) { v -> repo.update { it.copy(style = v) } }

        Spacer(Modifier.height(6.dp))
        Text("Show on photo", style = MaterialTheme.typography.bodyLarge, color = GpsSilver)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillChip("Map", settings.showMap) { repo.update { it.copy(showMap = !it.showMap) } }
            PillChip("Address", settings.showAddress) { repo.update { it.copy(showAddress = !it.showAddress) } }
            PillChip("Coordinates", settings.showCoords) { repo.update { it.copy(showCoords = !it.showCoords) } }
            PillChip("Date & time", settings.showDateTime) { repo.update { it.copy(showDateTime = !it.showDateTime) } }
            PillChip("Weather", settings.showWeather) { repo.update { it.copy(showWeather = !it.showWeather) } }
            PillChip("Altitude", settings.showAltitude) { repo.update { it.copy(showAltitude = !it.showAltitude) } }
            PillChip("Compass", settings.showHeading) { repo.update { it.copy(showHeading = !it.showHeading) } }
            PillChip("Accuracy", settings.showAccuracy) { repo.update { it.copy(showAccuracy = !it.showAccuracy) } }
            PillChip("Speed", settings.showSpeed) { repo.update { it.copy(showSpeed = !it.showSpeed) } }
            PillChip("Plus code", settings.showPlusCode) { repo.update { it.copy(showPlusCode = !it.showPlusCode) } }
            PillChip("Logo", settings.showLogo) { repo.update { it.copy(showLogo = !it.showLogo) } }
        }

        Spacer(Modifier.height(18.dp))
        TextButton(
            onClick = onMore,
            colors = ButtonDefaults.textButtonColors(contentColor = GpsTeal),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("All stamp settings")
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
