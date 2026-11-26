@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.wizlabs.gpscamera

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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

/** Colour that summarises GPS health: teal = fresh, amber = ageing / searching, red = off / stale / mock. */
internal fun gpsColor(fix: GpsFix?, hasLocation: Boolean): Color = when {
    !hasLocation -> GpsBad
    fix == null -> GpsAmber
    fix.isMock -> GpsBad
    fix.ageSeconds() <= 10 -> GpsGood
    fix.ageSeconds() <= 60 -> GpsAmber
    else -> GpsBad
}

internal fun zoomLabel(z: Float): String =
    if (z < 0.95f || abs(z - z.toInt()) > 0.05f) String.format(Locale.US, "%.1f×", z)
    else "${z.toInt()}×"

@Composable
internal fun GpsChip(
    fix: GpsFix?,
    hasLocation: Boolean,
    address: String?,
    tick: Long,
    clock: androidx.compose.runtime.State<Float>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Reading `tick` makes this recompose every second so the colour follows the fix age.
    @Suppress("UNUSED_VARIABLE") val unused = tick
    val color = gpsColor(fix, hasLocation)
    val locked = hasLocation && fix != null && !fix.isMock && fix.ageSeconds() <= 10
    val border by animateColorAsState(color.copy(alpha = .5f), tween(400), label = "gpsBorder")
    val text = when {
        !hasLocation -> "Location off"
        fix == null -> "Searching…"
        fix.isMock -> "Mock location"
        else -> {
            val acc = fix.accuracyM?.let { "±${it.toInt()} m" } ?: "GPS"
            "$acc · ${fix.ageSeconds()} s"
        }
    }
    val described = when {
        !hasLocation -> "Location is off"
        fix == null -> "Searching for GPS"
        fix.isMock -> "Mock location detected"
        else -> "Location accuracy " + (fix.accuracyM?.let { "${it.toInt()} metres, " } ?: "") + "${fix.ageSeconds()} seconds old"
    }
    val tickHaptic = rememberTick()
    androidx.compose.material3.Surface(
        onClick = { tickHaptic(); onClick() },
        shape = RoundedCornerShape(50),
        color = GpsGlass,
        contentColor = Color.White,
        border = BorderStroke(1.dp, border),
        modifier = modifier.widthIn(max = 132.dp).semantics { contentDescription = described }
    ) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            GpsLockGlyph(color = color, locked = locked, clock = clock)
            Spacer(Modifier.width(6.dp))
            // Crossfade only when the state changes (search → lock → stale), not on every number.
            AnimatedContent(
                targetState = text,
                contentKey = { locked to (fix == null) },
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "gpsText"
            ) { t ->
                Text(
                    t,
                    style = MonoSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun ZoomPresets(camera: Camera?, zoomRatio: Float, onSelect: (Float) -> Unit) {
    val zs = camera?.cameraInfo?.zoomState?.value
    val minZ = zs?.minZoomRatio ?: 1f
    val maxZ = zs?.maxZoomRatio ?: 1f
    val presets = buildList {
        if (minZ < 0.95f) add(minZ)
        add(1f)
        if (maxZ >= 2f) add(2f)
        if (maxZ >= 5f) add(5f)
    }
    if (presets.size < 2) {
        Spacer(Modifier.height(34.dp))
        return
    }
    // The chip at or below the current zoom is "active"; it shows the exact value while pinching.
    val activeIndex = presets.indexOfLast { it <= zoomRatio + 0.01f }.coerceAtLeast(0)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(GpsGlass)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEachIndexed { i, p ->
            val active = i == activeIndex
            val label = if (active) zoomLabel(zoomRatio) else zoomLabel(p)
            val scale by animateFloatAsState(if (active) 1f else 0.86f, tween(160), label = "zoomChip")
            val tick = rememberTick()
            Box(
                Modifier
                    .size(if (active) 40.dp else 34.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (active) Color(0x33FFFFFF) else Color.Transparent)
                    .clickable { tick(); onSelect(p) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) GpsTeal else Color.White,
                    fontSize = if (active) 13.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Quick row over the top of the preview: flash, timer, ratio, live GPS chip, grid, exposure.
 * Same actions and behaviour as before; [gpsChip] is the existing [GpsChip] passed in as a slot.
 */
@Composable
internal fun CameraQuickRow(
    settings: AppSettings,
    backLens: Boolean,
    exposureActive: Boolean,
    onFlash: () -> Unit,
    onTimer: () -> Unit,
    onAspect: () -> Unit,
    onGrid: () -> Unit,
    onExposure: () -> Unit,
    gpsChip: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = when (settings.flash) {
                FlashChoice.OFF -> Icons.Filled.FlashOff
                FlashChoice.AUTO -> Icons.Filled.FlashAuto
                FlashChoice.ON -> Icons.Filled.FlashOn
                FlashChoice.TORCH -> Icons.Filled.FlashlightOn
            },
            contentDescription = "Flash ${settings.flash.label}",
            active = settings.flash != FlashChoice.OFF && backLens,
            enabled = backLens,
            size = 40.dp, iconSize = 20.dp,
            onClick = onFlash
        )
        GlassIconButton(
            icon = when (settings.timerSeconds) {
                3 -> Icons.Filled.Timer3
                10 -> Icons.Filled.Timer10
                else -> Icons.Filled.Timer
            },
            contentDescription = "Self-timer",
            active = settings.timerSeconds > 0,
            size = 40.dp, iconSize = 20.dp,
            onClick = onTimer
        )
        GlassPill(text = settings.aspect.label, onClick = onAspect, modifier = Modifier.widthIn(min = 52.dp))
        gpsChip()
        GlassIconButton(
            icon = if (settings.gridOn) Icons.Filled.Grid3x3 else Icons.Filled.GridOff,
            contentDescription = "Grid",
            active = settings.gridOn,
            size = 40.dp, iconSize = 20.dp,
            onClick = onGrid
        )
        GlassIconButton(
            icon = Icons.Filled.Exposure,
            contentDescription = "Exposure",
            active = exposureActive,
            size = 40.dp, iconSize = 20.dp,
            onClick = onExposure
        )
    }
}
