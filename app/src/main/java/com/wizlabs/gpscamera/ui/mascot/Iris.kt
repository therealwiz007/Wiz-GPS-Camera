package com.wizlabs.gpscamera.ui.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.wizlabs.gpscamera.engine.Action
import com.wizlabs.gpscamera.engine.Expression
import com.wizlabs.gpscamera.engine.IrisBrain
import com.wizlabs.gpscamera.rememberMotionAllowed
import com.wizlabs.gpscamera.rememberThrottledClock
import kotlinx.coroutines.delay

/**
 * Iris, the Wiz GPS Camera mascot. Decorative: it never carries information that is not also text.
 * Code-drawn (no Lottie/Rive, no bitmaps). The shared clock is read only in the draw phase.
 * Returns a still frame when motion is off (reduce motion, Battery Saver, unfocused), 20 s after a search
 * starts, and 9 s after the last touch. Never place it on the camera preview, viewer or stamp.
 */
@Composable
fun Iris(
    expression: Expression,
    action: Action,
    size: Dp,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    val art = remember { IrisArt() }
    val pose = remember { IrisPose() }
    val allowed = rememberMotionAllowed()

    // Looping scan: 24 fps, capped at 20 s (never settles on touch for the search, but stops at the cap).
    var searchLive by remember(expression, action) { mutableStateOf(true) }
    LaunchedEffect(expression, action) {
        if (action == Action.SCAN) { delay(IrisBrain.SEARCH_ANIMATION_SEC * 1000L); searchLive = false }
    }
    val clock = rememberThrottledClock(running = allowed && action == Action.SCAN && searchLive, fps = 24, settle = false)

    // One-shot bursts (bounce 280 ms, sparkle 420 ms). Without motion the final pose is shown at once.
    val shot = remember(expression, action) { Animatable(if (allowed) 0f else 1f) }
    LaunchedEffect(expression, action, allowed) {
        val ms = when (action) { Action.BOUNCE -> 280; Action.SPARKLE -> 420; else -> 0 }
        if (allowed && ms > 0) { shot.snapTo(0f); shot.animateTo(1f, tween(ms, easing = LinearEasing)) } else shot.snapTo(1f)
    }

    var m = modifier.size(size).clearAndSetSemantics { }
    if (onTap != null) m = m.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap() }
    Canvas(m) {
        // Sparkle must be gone and bounce settled at rest, so a finished one-shot draws the neutral pose.
        val p = shot.value
        pose.update(expression, action, clock.value, if (p >= 1f) 1f else p)
        if (action == Action.BOUNCE && p >= 1f) { pose.bounceY = 0f; pose.squash = 1f }
        if (action == Action.SPARKLE && p >= 1f) pose.sparkle = 0f
        with(art) { draw(pose) }
    }
}
