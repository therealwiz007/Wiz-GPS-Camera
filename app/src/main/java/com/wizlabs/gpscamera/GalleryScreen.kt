@file:OptIn(ExperimentalFoundationApi::class)

package com.wizlabs.gpscamera

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

data class PhotoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int
)

private object VerifyCache {
    val map = HashMap<String, Integrity.Result>()
}

fun loadPhotos(context: Context): List<PhotoItem> {
    val out = ArrayList<PhotoItem>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT
    )
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
        arrayOf("Pictures/Wiz GPS Camera%"),
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )?.use { c ->
        val iId = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val iName = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val iTaken = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        val iAdded = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val iSize = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val iW = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val iH = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        while (c.moveToNext()) {
            val id = c.getLong(iId)
            val taken = c.getLong(iTaken)
            out.add(
                PhotoItem(
                    id = id,
                    uri = android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                    name = c.getString(iName) ?: "photo.jpg",
                    dateMs = if (taken > 0) taken else c.getLong(iAdded) * 1000L,
                    sizeBytes = c.getLong(iSize),
                    width = c.getInt(iW),
                    height = c.getInt(iH)
                )
            )
        }
    }
    return out
}

@Composable
fun GalleryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var viewer by remember { mutableStateOf<Int?>(null) }
    var infoFor by remember { mutableStateOf<PhotoItem?>(null) }
    var confirmDelete by remember { mutableStateOf<List<PhotoItem>?>(null) }

    LaunchedEffect(refresh) {
        val loadedItems = withContext(Dispatchers.IO) { loadPhotos(context) }
        items = loadedItems
        loaded = true
        selected = selected.intersect(loadedItems.map { it.id }.toSet())
        val v = viewer
        if (v != null && v >= loadedItems.size) viewer = if (loadedItems.isEmpty()) null else loadedItems.size - 1
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        selected = emptySet()
        refresh++
    }

    fun deleteNow(list: List<PhotoItem>) {
        val uris = list.map { it.uri }
        try {
            uris.forEach { context.contentResolver.delete(it, null, null) }
            selected = emptySet()
            refresh++
        } catch (e: SecurityException) {
            // Files created by an earlier install are not ours any more: ask the user to approve.
            val sender = if (Build.VERSION.SDK_INT >= 30) {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            } else {
                (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
            }
            if (sender != null) {
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                Toast.makeText(context, "Could not delete: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // The saved file itself is shared, byte for byte: nothing is re-encoded.
    fun share(list: List<PhotoItem>) {
        if (list.isEmpty()) return
        val intent = if (list.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, list[0].uri)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(list.map { it.uri }))
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Share photo"))
    }

    BackHandler {
        when {
            viewer != null -> viewer = null
            selected.isNotEmpty() -> selected = emptySet()
            else -> onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(WizNavy)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected.isEmpty()) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WizText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Gallery", color = WizText, style = MaterialTheme.typography.titleLarge)
                        if (items.isNotEmpty()) {
                            Text(
                                if (items.size == 1) "1 photo" else "${items.size} photos",
                                color = WizMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { selected = emptySet() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear selection", tint = WizText)
                    }
                    Text(
                        "${selected.size} selected",
                        color = WizText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { share(items.filter { it.id in selected }) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = WizTeal)
                    }
                    IconButton(onClick = { confirmDelete = items.filter { it.id in selected } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = WizBad)
                    }
                }
            }

            if (loaded && items.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(WizPanel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = WizTeal, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("No photos yet", color = WizText, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Every photo you take is stamped with place and time, and shows up here.",
                        color = WizMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(22.dp))
                    Button(onClick = onBack, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Take a photo")
                    }
                }
            } else {
                val groups = remember(items) { groupByDay(items) }
                if (selected.isEmpty() && items.isNotEmpty()) {
                    Text(
                        "Tip: press and hold a photo to select several",
                        color = WizMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 18.dp, bottom = 4.dp)
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groups.forEach { (label, dayItems) ->
                        item(key = "h_$label", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                label,
                                color = WizText,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 4.dp)
                            )
                        }
                        items(dayItems, key = { it.id }) { item ->
                            PhotoCell(
                                item = item,
                                isSelected = item.id in selected,
                                onClick = {
                                    if (selected.isNotEmpty()) {
                                        selected = if (item.id in selected) selected - item.id else selected + item.id
                                    } else {
                                        viewer = items.indexOf(item)
                                    }
                                },
                                onLongClick = {
                                    selected = if (item.id in selected) selected - item.id else selected + item.id
                                }
                            )
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
            }
        }

        val start = viewer
        if (start != null && items.isNotEmpty()) {
            Viewer(
                items = items,
                startIndex = start.coerceIn(0, items.size - 1),
                onClose = { viewer = null },
                onShare = { share(listOf(it)) },
                onDelete = { confirmDelete = listOf(it) },
                onInfo = { infoFor = it }
            )
        }
    }

    confirmDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(if (list.size == 1) "Delete this photo?" else "Delete ${list.size} photos?") },
            text = { Text("This removes the photo from your device. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    deleteNow(list)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }

    infoFor?.let { item -> InfoDialog(item) { infoFor = null } }
}

@Composable
private fun verifyState(item: PhotoItem): Integrity.Result? {
    val context = LocalContext.current
    val state by produceState<Integrity.Result?>(null, item.id, item.sizeBytes) {
        val key = "${item.id}:${item.sizeBytes}"
        val hit = VerifyCache.map[key]
        value = hit ?: withContext(Dispatchers.IO) {
            Integrity.verify(context, item.uri, item.name)
        }.also { VerifyCache.map[key] = it }
    }
    return state
}

@Composable
private fun PhotoCell(item: PhotoItem, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val thumb by produceState<Bitmap?>(null, item.id) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.loadThumbnail(item.uri, Size(320, 320), null) }.getOrNull()
        }
    }
    val verify = verifyState(item)
    val inset by animateFloatAsState(if (isSelected) 0.88f else 1f, tween(160), label = "cellInset")
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (isSelected) WizTeal.copy(alpha = .18f) else WizPanel)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(Modifier.fillMaxSize().scale(inset).clip(shape)) {
            thumb?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        VerifyBadge(verify, Modifier.align(Alignment.BottomEnd).padding(6.dp))
        if (isSelected) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(WizTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = WizNavy, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun VerifyBadge(result: Integrity.Result?, modifier: Modifier = Modifier) {
    val (icon, tint, label) = when (result) {
        Integrity.Result.VERIFIED -> Triple(Icons.Filled.Verified, WizGood, "Verified original")
        Integrity.Result.MODIFIED, Integrity.Result.LEDGER_TAMPERED -> Triple(Icons.Filled.Warning, WizBad, "Modified")
        else -> return
    }
    Box(
        modifier.size(22.dp).clip(CircleShape).background(Color(0xB3091923)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
    }
}

/** Verification as a readable chip for the full-screen viewer. */
@Composable
private fun VerifyChip(result: Integrity.Result?, modifier: Modifier = Modifier) {
    val (icon, tint, label) = when (result) {
        Integrity.Result.VERIFIED -> Triple(Icons.Filled.Verified, WizGood, "Verified original")
        Integrity.Result.MODIFIED -> Triple(Icons.Filled.Warning, WizBad, "Edited after capture")
        Integrity.Result.LEDGER_TAMPERED -> Triple(Icons.Filled.Warning, WizBad, "Record tampered")
        else -> return
    }
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xB3091923))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

/** "Today", "Yesterday" or a full date, newest first, keeping the input order inside each day. */
private fun groupByDay(items: List<PhotoItem>): List<Pair<String, List<PhotoItem>>> {
    val cal = Calendar.getInstance()
    fun dayKey(ms: Long): Int {
        cal.timeInMillis = ms
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }
    val today = dayKey(System.currentTimeMillis())
    val yesterday = dayKey(System.currentTimeMillis() - 86_400_000L)
    val fmt = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    val out = LinkedHashMap<String, MutableList<PhotoItem>>()
    for (item in items) {
        val k = dayKey(item.dateMs)
        val label = when (k) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> fmt.format(Date(item.dateMs))
        }
        out.getOrPut(label) { ArrayList() }.add(item)
    }
    return out.map { it.key to it.value }
}

@Composable
private fun Viewer(
    items: List<PhotoItem>,
    startIndex: Int,
    onClose: () -> Unit,
    onShare: (PhotoItem) -> Unit,
    onDelete: (PhotoItem) -> Unit,
    onInfo: (PhotoItem) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            val bmp by produceState<Bitmap?>(null, item.id) {
                value = withContext(Dispatchers.IO) { ImageUtil.decodeSampled(context, item.uri, 2048) }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val b = bmp
                if (b == null) CircularProgressIndicator(color = WizTeal)
                else Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        val current = items[pagerState.currentPage.coerceIn(0, items.size - 1)]
        val verify = verifyState(current)
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(current.dateMs)),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(current.dateMs)) +
                        "  ·  ${pagerState.currentPage + 1} of ${items.size}",
                    color = Color.White.copy(alpha = .7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            VerifyChip(verify, Modifier.padding(end = 8.dp))
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ViewerAction(Icons.Filled.Share, "Share") { onShare(current) }
            ViewerAction(Icons.Filled.Info, "Details") { onInfo(current) }
            ViewerAction(Icons.Filled.Delete, "Delete", tint = WizBad) { onDelete(current) }
        }
    }
}

@Composable
private fun ViewerAction(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    val tick = rememberTick()
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { tick(); onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun InfoDialog(item: PhotoItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val verify = verifyState(item)
    val record = remember(item.id) { Integrity.find(context, item.name) }
    val verdict = when (verify) {
        null -> "Checking…"
        Integrity.Result.VERIFIED -> "Verified: this is the unmodified file saved by this app on this device."
        Integrity.Result.MODIFIED -> "MODIFIED: the file no longer matches what the app saved."
        Integrity.Result.LEDGER_TAMPERED -> "Warning: the integrity record itself failed its signature check."
        Integrity.Result.UNKNOWN -> "Not in this device's ledger (taken before reinstall, or by another device)."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WizPanel,
        title = { Text("Photo details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text("${item.width} × ${item.height} · ${Formatter.formatShortFileSize(context, item.sizeBytes)}")
                Text(DateFormat.getDateTimeInstance().format(Date(item.dateMs)))
                if (record != null) {
                    Text("ID ${record.id}")
                    if (record.lat != null && record.lon != null) {
                        Text(String.format(java.util.Locale.US, "%.6f, %.6f", record.lat, record.lon))
                    }
                    if (record.mock) Text("Mock location was detected", color = Color(0xFFFFB74D))
                    if (record.note.isNotBlank()) Text("Note: ${record.note}")
                }
                Spacer(Modifier.height(4.dp))
                Text(verdict)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
