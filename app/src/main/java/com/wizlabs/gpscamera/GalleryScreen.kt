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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected.isEmpty()) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "Gallery (${items.size})",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    IconButton(onClick = { selected = emptySet() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear selection", tint = Color.White)
                    }
                    Text(
                        "${selected.size} selected",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { share(items.filter { it.id in selected }) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = { confirmDelete = items.filter { it.id in selected } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }

            if (loaded && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No photos yet.\nPhotos taken with this app appear here.",
                        color = Color.White.copy(alpha = .7f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(items, key = { it.id }) { item ->
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
    Box(
        Modifier
            .aspectRatio(1f)
            .background(Color(0xFF132630))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        thumb?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        VerifyBadge(verify, Modifier.align(Alignment.BottomEnd).padding(4.dp))
        if (isSelected) {
            Box(Modifier.fillMaxSize().background(WizTeal.copy(alpha = .35f)).border(3.dp, WizTeal))
            Icon(
                Icons.Filled.Verified,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(20.dp)
            )
        }
    }
}

@Composable
private fun VerifyBadge(result: Integrity.Result?, modifier: Modifier = Modifier) {
    when (result) {
        Integrity.Result.VERIFIED -> Icon(
            Icons.Filled.Verified, contentDescription = "Verified original",
            tint = Color(0xFF66FF99), modifier = modifier.size(20.dp)
        )
        Integrity.Result.MODIFIED, Integrity.Result.LEDGER_TAMPERED -> Icon(
            Icons.Filled.Warning, contentDescription = "Modified",
            tint = Color(0xFFFF5252), modifier = modifier.size(20.dp)
        )
        else -> {}
    }
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
            Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            VerifyBadge(verify, Modifier.padding(end = 12.dp))
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xB309141B))
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { onShare(current) }) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
            }
            IconButton(onClick = { onInfo(current) }) {
                Icon(Icons.Filled.Info, contentDescription = "Details", tint = Color.White)
            }
            IconButton(onClick = { onDelete(current) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
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
