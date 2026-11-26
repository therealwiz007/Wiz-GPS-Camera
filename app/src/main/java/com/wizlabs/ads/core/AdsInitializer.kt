package com.wizlabs.ads.core

import android.content.Context
import com.wizlabs.ads.AdsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** SDK-free stub (wiz-ads 0.3 --no-sdk): same public names as 0.2, no ad SDK. Never becomes ready. */
object AdsInitializer {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun ensure(context: Context, config: AdsConfig, canRequestAds: Boolean) = Unit
}
