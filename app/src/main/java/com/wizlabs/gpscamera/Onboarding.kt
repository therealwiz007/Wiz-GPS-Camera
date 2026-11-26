package com.wizlabs.gpscamera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.wizlabs.gpscamera.engine.IrisBrain
import com.wizlabs.gpscamera.engine.IrisEvent
import com.wizlabs.gpscamera.ui.mascot.Iris
import kotlinx.coroutines.launch

private data class OnboardPage(val title: String, val body: String)

private val PAGES = listOf(
    OnboardPage(
        "Proof of place and time",
        "Every photo is stamped with the address, GPS coordinates, map and network-checked time, on the image and in its EXIF data."
    ),
    OnboardPage(
        "Exact, fresh GPS",
        "The camera opens only after a fresh, precise fix. You always see the accuracy and how old the fix is."
    ),
    OnboardPage(
        "Untouched, verifiable photos",
        "Fake GPS apps are detected, and each saved photo gets a signed fingerprint, so edits show up as Edited in the gallery."
    )
)

/** First-run walkthrough (3 pages). Location and camera permissions are asked for right after it. */
@Composable
fun Onboarding(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.size - 1

    Column(
        Modifier
            .fillMaxSize()
            .background(GpsBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = onDone) { Text("Skip", color = GpsMuted) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = PAGES[index]
            val iris = IrisBrain.react(IrisEvent.Onboarding(index))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Iris(iris.expression, iris.action, 150.dp)
                Spacer(Modifier.height(20.dp))
                when (index) {
                    0 -> SampleStampPhoto(Modifier.padding(horizontal = 4.dp))
                    1 -> OnboardAccuracyDemo()
                    else -> OnboardShieldDemo()
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    page.title,
                    color = GpsSilver,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    page.body,
                    color = GpsMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(PAGES.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (i == pager.currentPage) 22.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) GpsTeal else GpsLine)
                )
            }
        }
        Button(
            onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GpsTeal, contentColor = GpsOnTeal),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).height(54.dp)
        ) {
            Text(if (last) "Allow location & camera" else "Next", style = MaterialTheme.typography.titleSmall, fontSize = 16.sp)
        }
    }
}

/** Static accuracy meter example: the same look as the GPS gate. */
@Composable
private fun OnboardAccuracyDemo() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(GpsSurface).padding(18.dp)
    ) {
        Text("±4 m · 2 s", color = GpsTeal, style = MonoBody)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(GpsSurfaceHi)) {
            Box(Modifier.fillMaxWidth(.92f).height(6.dp).clip(RoundedCornerShape(50)).background(GpsTeal))
        }
        Spacer(Modifier.height(10.dp))
        Text("Fresh fix", color = GpsMuted, style = MaterialTheme.typography.labelMedium)
    }
}

/** Verified badge example for page 3. */
@Composable
private fun OnboardShieldDemo() {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(GpsSurface).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = GpsTeal, modifier = Modifier.size(26.dp))
        Spacer(Modifier.size(10.dp))
        Text("Verified · untouched", color = GpsSilver, style = MaterialTheme.typography.titleSmall)
    }
}
