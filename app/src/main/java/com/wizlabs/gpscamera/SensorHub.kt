package com.wizlabs.gpscamera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Compass heading (the direction the back camera points, magnetic north) and a spirit-level
 * deviation (degrees away from the nearest horizontal/vertical axis).
 */
class SensorHub(context: Context) : SensorEventListener {
    private val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** Magnetic heading in degrees [0, 360), or null if there is no rotation sensor yet. */
    var headingDeg by mutableStateOf<Float?>(null)
        private set

    /** Degrees to rotate a horizontal line (clockwise positive) so it stays level with the horizon. */
    var levelDeg by mutableFloatStateOf(0f)
        private set

    /** False when the phone lies flat (level makes no sense) or there is no accelerometer. */
    var levelValid by mutableStateOf(false)
        private set

    private val rot = FloatArray(9)
    private var sinAvg = 0f
    private var cosAvg = 0f
    private var haveHeading = false
    private var ax = 0f
    private var ay = 0f
    private var az = 0f
    private var haveAcc = false
    private var lastLevelPublish = 0L

    fun start() {
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sm.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> onRotation(e.values)
            Sensor.TYPE_ACCELEROMETER -> onAcceleration(e.values)
        }
    }

    private fun onRotation(values: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rot, values)
        // World coordinates: x = East, y = North, z = Up. The back camera looks along -Z of the device.
        val ce = -rot[2]
        val cn = -rot[5]
        val horizontal = ce * ce + cn * cn
        val rad = if (horizontal > 0.1f) {
            atan2(ce, cn)
        } else {
            // Camera points almost straight up/down: fall back to the top edge of the phone.
            atan2(rot[1], rot[4])
        }
        if (!haveHeading) {
            sinAvg = sin(rad)
            cosAvg = cos(rad)
            haveHeading = true
        } else {
            sinAvg += 0.15f * (sin(rad) - sinAvg)
            cosAvg += 0.15f * (cos(rad) - cosAvg)
        }
        var deg = Math.toDegrees(atan2(sinAvg, cosAvg).toDouble()).toFloat()
        if (deg < 0f) deg += 360f
        val old = headingDeg
        if (old == null || abs(old - deg) >= 1f) headingDeg = deg
    }

    private fun onAcceleration(v: FloatArray) {
        if (!haveAcc) {
            ax = v[0]; ay = v[1]; az = v[2]
            haveAcc = true
        } else {
            ax += 0.15f * (v[0] - ax)
            ay += 0.15f * (v[1] - ay)
            az += 0.15f * (v[2] - az)
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastLevelPublish < 60) return
        lastLevelPublish = now
        val g = sqrt(ax * ax + ay * ay + az * az)
        val flat = g > 0.1f && abs(az) / g > 0.85f
        var dev = Math.toDegrees(atan2(ax, ay).toDouble()).toFloat()
        while (dev > 45f) dev -= 90f
        while (dev < -45f) dev += 90f
        levelValid = !flat
        if (abs(dev - levelDeg) > 0.2f) levelDeg = dev
    }
}
