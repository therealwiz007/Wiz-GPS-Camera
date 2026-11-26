package com.wizlabs.ads.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wizlabs.ads.AdPalette

/** SDK-free stub (wiz-ads 0.3 --no-sdk): every slot renders nothing and takes zero height. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun WizBannerAd(screen: String, modifier: Modifier = Modifier, inline: Boolean = false) = Unit

@Composable
@Suppress("UNUSED_PARAMETER")
fun WizNativeAdCard(screen: String, palette: AdPalette, modifier: Modifier = Modifier) = Unit

/** Always runs [then] exactly once, straight away. */
@Suppress("UNUSED_PARAMETER")
fun showInterstitialAtBreak(screen: String, then: () -> Unit) = then()
