package com.wizlabs.ads.core

import android.content.Context
import android.widget.FrameLayout
import com.wizlabs.ads.AdPalette

/** SDK-free stub (wiz-ads 0.3 --no-sdk): an empty container; bind and destroy do nothing. */
class WizNativeAdView(context: Context, @Suppress("UNUSED_PARAMETER") private val palette: AdPalette) {
    val root: FrameLayout = FrameLayout(context)

    @Suppress("UNUSED_PARAMETER")
    fun bind(ad: NativeAd) = Unit

    fun destroy() = Unit
}
