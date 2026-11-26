package com.wizlabs.ads.core

import com.wizlabs.ads.AdsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local placeholder for the SDK's NativeAd (wiz-ads 0.3 --no-sdk). Can never be constructed. */
class NativeAd private constructor()

/** SDK-free stub (wiz-ads 0.3 --no-sdk): nothing is ever loaded. */
object NativeAdPool {
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun preload(config: AdsConfig) = Unit

    fun take(): NativeAd? = null

    fun clear() = Unit
}
