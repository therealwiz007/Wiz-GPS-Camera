package com.wizlabs.gpscamera

import androidx.camera.core.Camera
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

/**
 * Bottom controls: zoom presets, then gallery thumbnail (left), shutter (centre), flip camera (right).
 * The pills that used to sit above the shutter (project, note, stamp) moved to [CameraTopBar].
 */
@Composable
fun CameraShutterBar(
    camera: Camera?,
    zoomRatio: Float,
    onZoom: (Float) -> Unit,
    thumb: ImageBitmap?,
    thumbScale: Animatable<Float, *>,
    onThumbBounds: (Rect) -> Unit,
    onGallery: () -> Unit,
    shutterReady: Boolean,
    shutterBusy: Boolean,
    burst: Int,
    captureKey: Int,
    onShoot: () -> Unit,
    backLens: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tickHaptic = rememberTick()
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6060B18), GpsBg)))
            .navigationBarsPadding()
            .padding(top = 26.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.enterRise(2)) { ZoomPresets(camera = camera, zoomRatio = zoomRatio, onSelect = onZoom) }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Last photo -> gallery
            Box(
                Modifier
                    .size(52.dp)
                    .onGloballyPositioned { onThumbBounds(it.boundsInRoot()) }
                    .graphicsLayer { scaleX = thumbScale.value; scaleY = thumbScale.value }
                    .clip(RoundedCornerShape(12.dp))
                    .background(GpsSurface)
                    .border(2.dp, GpsSilver.copy(alpha = .85f), RoundedCornerShape(12.dp))
                    .clickable { tickHaptic(); onGallery() },
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(thumb, contentDescription = "Open gallery", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Open gallery", tint = GpsSilver)
                }
            }

            ShutterButton(ready = shutterReady, busy = shutterBusy, burst = burst, captureKey = captureKey, onClick = onShoot)

            val flipAngle by animateFloatAsState(
                if (backLens) 0f else 180f,
                spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                label = "flip"
            )
            GlassIconButton(
                icon = Icons.Filled.Cameraswitch,
                contentDescription = "Switch camera",
                size = 52.dp,
                iconSize = 26.dp,
                modifier = Modifier.rotate(flipAngle),
                onClick = onFlip
            )
        }
    }
}
