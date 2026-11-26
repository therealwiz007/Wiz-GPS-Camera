package com.wizlabs.gpscamera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// All art here is drawn in code. Paths/brushes are cached in drawWithCache; per-frame work is a
// handful of primitive draws reading a throttled clock in the draw phase.

/**
 * GPS status glyph: sonar rings ripple out of the dot while searching; once locked it settles
 * into a steady reticle with a one-shot "lock" pulse.
 */
@Composable
fun GpsLockGlyph(color: Color, locked: Boolean, clock: State<Float>, modifier: Modifier = Modifier) {
    val system = rememberSystemMotion()
    val lockPop = remember { Animatable(1f) }
    LaunchedEffect(locked) {
        if (locked && system) {
            lockPop.snapTo(0f)
            lockPop.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        }
    }
    Spacer(
        modifier
            .size(20.dp)
            .drawBehind {
                val c = center
                val d = density
                val core = 3.2f * d
                if (!locked) {
                    val t = clock.value
                    for (k in 0..1) {
                        val p = (t * 0.8f + k * 0.5f) % 1f
                        drawCircle(
                            color.copy(alpha = (1f - p) * 0.85f),
                            radius = core + p * 6.5f * d,
                            center = c,
                            style = Stroke(1.3f * d)
                        )
                    }
                } else {
                    val r = 6.2f * d
                    val g = 2.2f * d
                    val w = 1.4f * d
                    drawCircle(color.copy(alpha = .55f), r, c, style = Stroke(1.2f * d))
                    drawLine(color, Offset(c.x, c.y - r - g), Offset(c.x, c.y - r + g), w)
                    drawLine(color, Offset(c.x, c.y + r - g), Offset(c.x, c.y + r + g), w)
                    drawLine(color, Offset(c.x - r - g, c.y), Offset(c.x - r + g, c.y), w)
                    drawLine(color, Offset(c.x + r - g, c.y), Offset(c.x + r + g, c.y), w)
                    val p = lockPop.value
                    if (p < 1f) {
                        drawCircle(color.copy(alpha = (1f - p) * .9f), r + p * 6f * d, c, style = Stroke(1.6f * d))
                    }
                }
                drawCircle(color, core, c)
            }
    )
}

/** Small compass rose whose needle points to true north; turns along the shortest way. */
@Composable
fun CompassRose(heading: Float, modifier: Modifier = Modifier) {
    val angle = remember { Animatable(-heading) }
    LaunchedEffect(heading) {
        var delta = (-heading - angle.value) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        angle.animateTo(angle.value + delta, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }
    Spacer(
        modifier
            .size(24.dp)
            .drawWithCache {
                val r = size.minDimension / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val north = Path().apply {
                    moveTo(cx, cy - r * 0.80f); lineTo(cx + r * 0.24f, cy); lineTo(cx - r * 0.24f, cy); close()
                }
                val south = Path().apply {
                    moveTo(cx, cy + r * 0.80f); lineTo(cx + r * 0.24f, cy); lineTo(cx - r * 0.24f, cy); close()
                }
                val ring = Stroke(1.dp.toPx())
                val tickW = 1.dp.toPx()
                val tickIn = 1.6.dp.toPx()
                val tickLong = 4.dp.toPx()
                val tickShort = 2.6.dp.toPx()
                val hub = 1.6.dp.toPx()
                onDrawBehind {
                    drawCircle(Color.White.copy(alpha = .08f), r)
                    drawCircle(Color.White.copy(alpha = .28f), r - tickW / 2f, style = ring)
                    rotate(angle.value) {
                        for (i in 0 until 8) {
                            val major = i % 2 == 0
                            rotate(i * 45f) {
                                drawLine(
                                    Color.White.copy(alpha = if (major) .7f else .3f),
                                    Offset(cx, cy - r + tickIn),
                                    Offset(cx, cy - r + if (major) tickLong else tickShort),
                                    tickW
                                )
                            }
                        }
                        drawPath(south, Color.White.copy(alpha = .7f))
                        drawPath(north, GpsTeal)
                    }
                    drawCircle(GpsBg, hub)
                }
            }
    )
}

/**
 * Viewfinder corner brackets. They glide in from outside and settle to a quiet alpha whenever
 * [appearKey] changes (camera start, lens flip, aspect change). No continuous motion.
 */
@Composable
fun ViewfinderCorners(appearKey: Any, modifier: Modifier = Modifier) {
    val system = rememberSystemMotion()
    val p = remember { Animatable(1f) }
    LaunchedEffect(appearKey) {
        if (system) {
            p.snapTo(0f)
            p.animateTo(1f, tween(620, easing = WizMotion.Emphasized))
        }
    }
    Spacer(
        modifier
            .fillMaxSize()
            .drawBehind {
                val v = p.value
                val inset = 14.dp.toPx() - (1f - v) * 10.dp.toPx()
                val len = 22.dp.toPx() + (1f - v) * 14.dp.toPx()
                val sw = 2.4.dp.toPx()
                val glow = sin(v * PI).toFloat()
                val c = (if (glow > .05f) GpsTeal else Color.White).copy(alpha = .38f + .5f * glow)
                val w = size.width
                val h = size.height
                fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                    drawLine(c, Offset(x, y), Offset(x + dx * len, y), sw, StrokeCap.Round)
                    drawLine(c, Offset(x, y), Offset(x, y + dy * len), sw, StrokeCap.Round)
                }
                corner(inset, inset, 1f, 1f)
                corner(w - inset, inset, -1f, 1f)
                corner(inset, h - inset, 1f, -1f)
                corner(w - inset, h - inset, -1f, -1f)
            }
    )
}

/**
 * Shutter: inner disc presses in with a springy release; each capture fires a teal ring burst;
 * while saving a short arc orbits the rim (that's progress, not decoration).
 */
@Composable
fun ShutterButton(ready: Boolean, busy: Boolean, burst: Int, captureKey: Int, onClick: () -> Unit) {
    val tick = rememberTick()
    val system = rememberSystemMotion()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val inner by animateFloatAsState(
        if (pressed) 0.78f else 1f,
        if (pressed) tween(80) else spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium),
        label = "shutterInner"
    )
    val rimAlpha by animateFloatAsState(if (ready || busy) 1f else .45f, tween(200), label = "rim")
    val burstRing = remember { Animatable(1f) }
    LaunchedEffect(captureKey) {
        if (captureKey > 0 && system) {
            burstRing.snapTo(0f)
            burstRing.animateTo(1f, tween(560, easing = LinearOutSlowInEasing))
        }
    }
    val spin = rememberThrottledClock(running = busy && system, settle = false)
    Box(
        Modifier
            .size(84.dp)
            .drawBehind {
                val b = burstRing.value
                if (b < 1f) {
                    drawCircle(
                        GpsTeal.copy(alpha = (1f - b) * .9f),
                        radius = size.minDimension / 2f + b * 24.dp.toPx(),
                        style = Stroke((1f + 3f * (1f - b)) * density)
                    )
                }
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = ready,
                onClick = { tick(); onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val sw = 3.dp.toPx()
                    val inset = 4.dp.toPx()
                    drawCircle(GpsTeal.copy(alpha = rimAlpha), size.minDimension / 2f - inset - sw / 2f, style = Stroke(sw))
                    if (busy) {
                        val start = if (system) (spin.value * 420f) % 360f else -90f
                        drawArc(
                            GpsSilver, start, 110f, false,
                            topLeft = Offset(inset + sw / 2f, inset + sw / 2f),
                            size = Size(size.width - 2 * inset - sw, size.height - 2 * inset - sw),
                            style = Stroke(sw, cap = StrokeCap.Round)
                        )
                    }
                }
        )
        Box(
            Modifier
                .size(62.dp)
                .graphicsLayer {
                    scaleX = inner
                    scaleY = inner
                }
                .clip(CircleShape)
                .background(if (ready || busy) GpsSilver else GpsMuted)
        )
        if (burst > 1 && !busy) {
            Text("×$burst", color = GpsBg, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/**
 * Hero for the "waiting for GPS" screen: three satellites orbit the app icon on dashed tracks,
 * a sweep beam turns and sonar rings ripple out until a fix arrives. 30 fps max, still frame
 * when motion is off.
 */
@Composable
fun SatelliteLockHero(searching: Boolean, modifier: Modifier = Modifier, center: @Composable () -> Unit) {
    val motion = rememberMotionAllowed()
    val clock = rememberThrottledClock(running = motion, settle = !searching)
    Box(modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Spacer(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val maxR = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val glow = Brush.radialGradient(
                        listOf(GpsTeal.copy(alpha = .20f), GpsTeal.copy(alpha = .04f), Color.Transparent),
                        center = center, radius = maxR
                    )
                    val sweep = Brush.sweepGradient(
                        listOf(Color.Transparent, Color.Transparent, GpsTeal.copy(alpha = .0f), GpsTeal.copy(alpha = .30f)),
                        center = center
                    )
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
                    val trackStroke = Stroke(1.2.dp.toPx(), pathEffect = dash)
                    val ringStroke = Stroke(2.dp.toPx())
                    val orbits = floatArrayOf(.56f, .74f, .92f)
                    val speeds = floatArrayOf(.11f, -.07f, .05f)
                    val offsets = floatArrayOf(.10f, .55f, .82f)
                    onDrawBehind {
                        val t = clock.value
                        drawCircle(glow, maxR)
                        for (o in orbits) drawCircle(GpsTeal.copy(alpha = .22f), maxR * o, style = trackStroke)
                        if (searching) {
                            rotate((t * 70f) % 360f) { drawCircle(sweep, maxR * .92f) }
                            for (i in 0 until 3) {
                                val p = (t / 2.4f + i / 3f) % 1f
                                drawCircle(
                                    GpsTeal.copy(alpha = (1f - p) * .45f),
                                    radius = maxR * (0.32f + 0.68f * p),
                                    style = ringStroke
                                )
                            }
                        }
                        for (i in orbits.indices) {
                            val a = 2f * PI.toFloat() * (offsets[i] + t * speeds[i])
                            val r = maxR * orbits[i]
                            satellite(Offset(center.x + r * cos(a), center.y + r * sin(a)), a, searching)
                        }
                    }
                }
        )
        center()
    }
}

private fun DrawScope.satellite(at: Offset, angle: Float, searching: Boolean) {
    val d = density
    val body = 3.4f * d
    val c = if (searching) GpsTeal else GpsGood
    rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f, at) {
        drawLine(c.copy(alpha = .7f), Offset(at.x - 9f * d, at.y), Offset(at.x + 9f * d, at.y), 2.6f * d)
        drawCircle(GpsBg, body + 1.4f * d, at)
        drawCircle(c, body, at)
    }
}
