package com.wizlabs.ads.core

import android.app.Activity
import com.wizlabs.ads.AdsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** SDK-free stub (wiz-ads 0.3 --no-sdk): no consent form, no UMP. Callbacks run at once. */
class ConsentManager {
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun gather(activity: Activity, config: AdsConfig, onDone: () -> Unit) = onDone()

    @Suppress("UNUSED_PARAMETER")
    fun showPrivacyOptions(activity: Activity, config: AdsConfig, onClosed: () -> Unit) = onClosed()
}
