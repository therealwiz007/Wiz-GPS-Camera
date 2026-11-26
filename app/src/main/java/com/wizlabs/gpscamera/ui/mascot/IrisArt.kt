package com.wizlabs.gpscamera.ui.mascot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

private val Teal = Color(0xFF00E5C7)
private val TealDeep = Color(0xFF00B39C)
private val Silver = Color(0xFFEAF2FF)
private val Glass = Color(0xFF060B18)

/**
 * Immutable Iris geometry in a 100×100 unit box, built once (handoff §13): a teal map pin 54 × 72 whose head
 * is a camera lens (silver ring, navy glass) holding two eyes. Same numbers as store-assets/mascot/iris.svg.
 */
class IrisArt {
    private val cxL = 42.5f
    private val cxR = 57.5f
    private val cy = 34f

    private val pin = Path().apply {
        moveTo(50f, 78f)
        cubicTo(50f, 78f, 23f, 54f, 23f, 33f)
        arcTo(Rect(23f, 6f, 77f, 60f), 180f, 180f, false)
        cubicTo(77f, 54f, 50f, 78f, 50f, 78f)
        close()
    }
    private val pinBrush = Brush.linearGradient(listOf(Teal, TealDeep), Offset(50f, 6f), Offset(50f, 78f))
    private val glint = Path().apply { moveTo(35.5f, 32f); quadraticTo(37f, 26f, 43f, 24.5f) }
    private val happyL = arc(cxL, cy, 1f)
    private val happyR = arc(cxR, cy, 1f)
    private val winkR = arc(cxR, cy, -1f)
    private val browL = Path().apply { moveTo(37.5f, 27f); quadraticTo(42.5f, 24.2f, 47f, 26.5f) }
    private val browR = Path().apply { moveTo(53f, 26.5f); quadraticTo(57.5f, 24.2f, 62.5f, 27f) }
    private val star = Path().apply {
        // 4-point sparkle centred on (0,0), radius 5.
        moveTo(0f, -5f); quadraticTo(0.8f, -0.8f, 5f, 0f); quadraticTo(0.8f, 0.8f, 0f, 5f); quadraticTo(-0.8f, 0.8f, -5f, 0f); quadraticTo(-0.8f, -0.8f, 0f, -5f)
        close()
    }

    private fun arc(cx: Float, cy: Float, dir: Float) = Path().apply {
        moveTo(cx - 4f, cy + 2f * dir); quadraticTo(cx, cy - 5f * dir, cx + 4f, cy + 2f * dir)
    }

    /** Draws the mascot filling [size]. Reads only [pose]; allocates nothing. */
    fun DrawScope.draw(pose: IrisPose) {
        scale(size.minDimension / 100f, Offset.Zero) {
            // Ground ellipse and the optional search ripple stay on the ground.
            drawOval(Teal.copy(alpha = .25f), Offset(28f, 80.5f), Size(44f, 9f))
            if (pose.ripple >= 0f) {
                val r = pose.ripple
                drawOval(Teal.copy(alpha = .3f * (1f - r)), Offset(28f - 12f * r, 80.5f - 2.5f * r), Size(44f + 24f * r, 9f + 5f * r), style = Stroke(1.2f))
            }
            translate(0f, pose.bounceY) {
                scale(1f + (1f - pose.squash) * 0.6f, pose.squash, Offset(50f, 78f)) {
                    drawPath(pin, pinBrush)
                    drawCircle(Silver, 22f, Offset(50f, 33f))
                    drawCircle(Glass, 18f, Offset(50f, 33f))
                    drawPath(glint, Teal, style = Stroke(2.2f, cap = StrokeCap.Round))
                    val s = Stroke(2.4f, cap = StrokeCap.Round)
                    val brow = Stroke(1.6f, cap = StrokeCap.Round)
                    translate(0f, -pose.browLift * 0.4f) {
                        rotate(-pose.tiltDeg * .6f, Offset(42f, 26f)) { drawPath(browL, Silver, style = brow) }
                        rotate(pose.tiltDeg * .6f, Offset(58f, 26f)) { drawPath(browR, Silver, style = brow) }
                    }
                    when {
                        pose.sleepy > 0f -> {
                            drawLine(Silver, Offset(cxL - 3.5f, cy + 1f), Offset(cxL + 3.5f, cy + 1f), 2.2f, StrokeCap.Round)
                            drawLine(Silver, Offset(cxR - 3.5f, cy + 1f), Offset(cxR + 3.5f, cy + 1f), 2.2f, StrokeCap.Round)
                        }
                        pose.happy > 0f -> {
                            drawPath(happyL, Silver, style = s); drawPath(happyR, Silver, style = s)
                        }
                        else -> {
                            eye(cxL, -pose.tiltDeg, pose)
                            if (pose.winkR > 0f) drawPath(winkR, Silver, style = s) else eye(cxR, pose.tiltDeg, pose)
                        }
                    }
                }
                if (pose.sparkle > 0f) translate(70f, 12f) {
                    scale(0.6f + 0.6f * pose.sparkle, Offset.Zero) { drawPath(star, Teal.copy(alpha = pose.sparkle)) }
                }
            }
        }
    }

    private fun DrawScope.eye(cx: Float, tilt: Float, pose: IrisPose) {
        val x = cx + pose.pupilX
        val y = cy + pose.pupilY
        rotate(tilt, Offset(x, y)) {
            drawOval(Silver, Offset(x - 3f, y - 4.5f * pose.eyeH), Size(6f, 9f * pose.eyeH))
        }
    }
}
