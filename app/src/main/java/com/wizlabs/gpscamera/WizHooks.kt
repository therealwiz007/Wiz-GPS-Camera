package com.wizlabs.gpscamera

import android.app.Activity
import androidx.compose.ui.graphics.toArgb
import com.wizlabs.update.UpdatePalette
import com.wizlabs.update.WizUpdate

/** UI state read outside Compose (set from the app composable). */
internal object GpsUi {
    /** True while onboarding, the GPS gate or the camera screen is showing: no update popup then. */
    @Volatile var busy = false
}

internal fun createWizUpdate(activity: Activity): WizUpdate = WizUpdate(activity, object : WizUpdate.Host {
    override fun palette() = UpdatePalette(
        GpsSurface.toArgb(), GpsLine.toArgb(), GpsSilver.toArgb(), GpsMuted.toArgb(), GpsTeal.toArgb(), GpsOnTeal.toArgb(), 20
    )
    /** Never while the camera is on screen (capture, focus, timer). */
    override fun mayPromptNow() = !GpsUi.busy
    override fun appName() = activity.getString(R.string.app_full_name)
    override fun restartNote(): String? = null
}, BuildConfig.DEBUG)
