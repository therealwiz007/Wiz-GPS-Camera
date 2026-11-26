package com.wizlabs.ads.core

import android.app.Activity
import android.widget.FrameLayout

/** SDK-free stub (wiz-ads 0.3 --no-sdk): an empty zero-height container; load reports a failure. */
class BannerHost(activity: Activity) {
    val view: FrameLayout = FrameLayout(activity)

    @Suppress("UNUSED_PARAMETER")
    fun load(unitId: String, widthDp: Int, onLoaded: (heightPx: Int) -> Unit, onFailed: () -> Unit) = onFailed()

    fun destroy() { view.removeAllViews() }
}
