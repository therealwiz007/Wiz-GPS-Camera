package com.wizlabs.ads

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import com.wizlabs.ads.core.AdFormat
import com.wizlabs.ads.core.AdGate
import com.wizlabs.ads.core.AdState
import com.wizlabs.ads.core.AdsInitializer
import com.wizlabs.ads.core.ConsentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Version of the shared package; stamped by `wiz-ads/sync.sh`. Never edit the copy inside an app.
 * 0.3 adds `sync.sh --no-sdk`: the six SDK-touching files are replaced by no-op stubs (stub/) and
 * [WIZ_ADS_SDK_LINKED] becomes false in the copy. The 0.2 SDK files themselves are unchanged.
 */
const val WIZ_ADS_VERSION = "0.3"

/**
 * True in builds that link the ad SDK (Next-Gen GMA + UMP). From 0.2 every app that syncs this
 * package must have both dependencies. Whether anything runs is still decided by
 * [AdsConfig.enabled] (BuildConfig.WIZ_ADS_ENABLED): while that is false the SDK and UMP are
 * never touched. Billing, rewarded, Premium and the notice sheet are not in 0.2 (plan 4.2).
 */
const val WIZ_ADS_SDK_LINKED = false

/**
 * One entry point per app (plan 2.3). `install` is cheap and does no SDK work; it may run in
 * onCreate. With ads switched off, nothing here touches the SDK, UMP or the network.
 */
object WizAds {
    private var config: AdsConfig? = null
    private var prefs: AdsPrefs? = null
    private var gate: AdGate? = null
    private var appCtx: Context? = null
    private var consent: ConsentManager? = null

    private val _screen = MutableStateFlow("")
    val currentScreen: StateFlow<String> = _screen.asStateFlow()

    /** False while ads are off, in grace, Pro, ad-free window, no consent, etc. Slots render nothing then. */
    private val _adsVisible = MutableStateFlow(false)
    val adsVisible: StateFlow<Boolean> = _adsVisible.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** Show the "Privacy options" menu entry only while this is true (always false with ads off). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** True only in a build with ads switched on. */
    val enabled: Boolean get() = config?.enabled == true

    fun configOrNull(): AdsConfig? = config

    fun install(app: Context, config: AdsConfig) {
        this.config = config
        this.appCtx = app.applicationContext
        this.prefs = AdsPrefs(app)
        this.gate = AdGate(config)
        this.consent = ConsentManager()
    }

    /**
     * Counts sessions (a cold start, or coming back after 30+ minutes away) and, after the first
     * frame, gathers consent. Returns at once, before any prefs or consent work, while ads are off.
     */
    fun onActivityStart(activity: Activity) {
        val c = config ?: return
        if (!c.enabled) return
        val p = prefs ?: return
        val now = System.currentTimeMillis()
        if (p.adsEraStart == 0L) p.adsEraStart = now      // grace counts from the first ads-capable launch
        if (p.lastForegroundAt == 0L || now - p.lastForegroundAt > SESSION_GAP_MS) {
            p.sessions = p.sessions + 1
            p.sessionStartedAt = now
        }
        p.lastForegroundAt = now
        refresh()
        val cm = consent ?: return
        activity.window.decorView.post {
            activity.window.decorView.postDelayed({
                if (activity.isFinishing || activity.isDestroyed) return@postDelayed
                cm.gather(activity, c) {
                    _privacyOptionsRequired.value = cm.privacyOptionsRequired.value
                    refresh()
                }
            }, CONSENT_DELAY_MS)
        }
    }

    fun onActivityStop() {
        if (!enabled) return
        prefs?.lastForegroundAt = System.currentTimeMillis()
    }

    /** The app calls this from its navigation, with a stable screen name. */
    fun onScreen(name: String) {
        _screen.value = name
        refresh()
    }

    fun mayShow(format: AdFormat, screen: String): Boolean {
        val g = gate ?: return false
        return g.mayShow(format, screen, state() ?: return false)
    }

    /** Why nothing shows right now (for debug). */
    fun whyNot(format: AdFormat, screen: String): String =
        state()?.let { gate?.reason(it, format, screen) } ?: "not installed"

    /** Opens the UMP privacy options form. No-op while ads are off. */
    fun showPrivacyOptions(activity: Activity) {
        val c = config ?: return
        if (!c.enabled) return
        val cm = consent ?: return
        cm.showPrivacyOptions(activity, c) { _privacyOptionsRequired.value = cm.privacyOptionsRequired.value; refresh() }
    }

    /** Called by a slot that is about to appear: initialises the SDK once, off the main thread. */
    internal fun ensureInitialized(context: Context) {
        val c = config ?: return
        AdsInitializer.ensure(context, c, consent?.canRequestAds?.value == true)
    }

    private fun refresh() {
        val g = gate ?: return
        _adsVisible.value = state()?.let { g.adsAllowedAtAll(it) } ?: false
    }

    private fun state(): AdState? {
        val p = prefs ?: return null
        val ctx = appCtx ?: return null
        val now = System.currentTimeMillis()
        return AdState(
            now = now,
            graceStart = maxOf(installTime(ctx), p.adsEraStart.takeIf { it > 0 } ?: now),
            sessions = p.sessions,
            sessionStartedAt = p.sessionStartedAt,
            proOwned = p.proOwned,
            adFreeUntil = p.adFreeUntil,
            consentOk = consent?.canRequestAds?.value == true,
            sdkLinked = WIZ_ADS_SDK_LINKED,
            lowRam = runCatching { (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice }.getOrDefault(true),
            batterySaver = runCatching { (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode }.getOrDefault(false),
        )
    }

    private fun installTime(ctx: Context): Long = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).firstInstallTime
    }.getOrDefault(0L)

    private const val SESSION_GAP_MS = 30 * 60_000L
    private const val CONSENT_DELAY_MS = 1_500L
}
