package com.wizlabs.gpscamera.ui.mascot

import com.wizlabs.gpscamera.engine.Action
import com.wizlabs.gpscamera.engine.Expression
import kotlin.math.PI
import kotlin.math.sin

/** Mutable bag of numbers the drawing code reads. Updated in place each frame: no allocation. */
class IrisPose {
    var pupilX = 0f      // eye offset in art units
    var pupilY = 0f
    var eyeH = 1f        // 1 = fully open oval
    var happy = 0f       // 1 = both eyes are ^ arcs
    var winkR = 0f       // 1 = right eye is a wink arc
    var sleepy = 0f      // 1 = both eyes are closed lines
    var tiltDeg = 0f     // eye tilt, worried
    var browLift = 0f
    var bounceY = 0f
    var squash = 1f
    var sparkle = 0f     // 0..1 sparkle alpha
    var ripple = -1f     // <0 = no ripple ring, else 0..1 phase

    /**
     * @param t animation seconds (frozen when motion is off), @param oneShot 0..1 progress of a one-shot action.
     * Pure function of its inputs, so a still frame is simply t = 0 and oneShot = 1.
     */
    fun update(expression: Expression, action: Action, t: Float, oneShot: Float) {
        pupilX = 0f; pupilY = 0f; eyeH = 1f; happy = 0f; winkR = 0f; sleepy = 0f
        tiltDeg = 0f; browLift = 0f; bounceY = 0f; squash = 1f; sparkle = 0f; ripple = -1f
        when (expression) {
            Expression.NEUTRAL -> Unit
            Expression.SEARCHING -> {
                if (action == Action.SCAN) {
                    pupilX = sin(t * 2.4f) * 4f
                    pupilY = sin(t * 1.3f) * 1.5f
                    ripple = (t * 0.9f) % 1f
                } else pupilX = 2f
                browLift = 1.2f
            }
            Expression.HAPPY -> happy = 1f
            Expression.PROUD -> { happy = 0f; winkR = 1f }
            Expression.WORRIED -> { tiltDeg = 14f; browLift = 2f; pupilY = 1f }
            Expression.SLEEPY -> sleepy = 1f
        }
        when (action) {
            Action.BOUNCE -> {
                val p = oneShot.coerceIn(0f, 1f)
                bounceY = -7f * sin(PI.toFloat() * p)
                squash = 1f - 0.05f * sin(2f * PI.toFloat() * p)
            }
            Action.SPARKLE -> sparkle = sin(PI.toFloat() * oneShot.coerceIn(0f, 1f))
            else -> Unit
        }
    }
}
