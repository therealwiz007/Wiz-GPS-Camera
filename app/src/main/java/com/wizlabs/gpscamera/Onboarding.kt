package com.wizlabs.gpscamera

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardPage(val icon: ImageVector?, val title: String, val body: String)

private val PAGES = listOf(
    OnboardPage(
        null,
        "Proof in every photo",
        "Each photo is stamped with the address, GPS coordinates, map and network-checked time — right on the image and in its EXIF data."
    ),
    OnboardPage(
        Icons.Filled.FolderOpen,
        "Organise by project",
        "File photos under a job or site, add quick tags like Before / After, and find them later on a map of every shot."
    ),
    OnboardPage(
        Icons.Filled.PictureAsPdf,
        "Reports in one tap",
        "Turn a project into a PDF report, or export a spreadsheet (CSV) or Google Earth file (KML) to share with clients."
    ),
    OnboardPage(
        Icons.Filled.VerifiedUser,
        "Tamper-evident",
        "Fake GPS apps are detected, and every saved photo gets a signed fingerprint, so edits show up as Modified."
    )
)

/** First-run walkthrough. Location and camera permissions are asked for right after it. */
@Composable
fun Onboarding(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.size - 1

    Column(
        Modifier
            .fillMaxSize()
            .background(WizNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = onDone) { Text("Skip", color = Color.White.copy(alpha = .7f)) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = PAGES[index]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(132.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(WizPanel),
                    contentAlignment = Alignment.Center
                ) {
                    if (page.icon == null) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                    } else {
                        Icon(page.icon, contentDescription = null, tint = WizTeal, modifier = Modifier.size(64.dp))
                    }
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    page.title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    page.body,
                    color = Color.White.copy(alpha = .75f),
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
                        .background(if (i == pager.currentPage) WizTeal else Color.White.copy(alpha = .3f))
                )
            }
        }
        Button(
            onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            colors = ButtonDefaults.buttonColors(containerColor = WizTeal, contentColor = WizNavy),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).height(52.dp)
        ) {
            Text(if (last) "Get started" else "Next", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.width(1.dp))
    }
}
