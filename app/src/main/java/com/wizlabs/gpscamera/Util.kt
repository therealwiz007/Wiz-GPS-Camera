package com.wizlabs.gpscamera

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

object ImageUtil {
    /** Decode an image no larger than [maxDim] on its longest side. */
    fun decodeSampled(context: Context, uri: Uri, maxDim: Int): Bitmap? = try {
        val cr = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) {
        null
    }
}

/** The optional user-supplied logo that is printed on the stamp. Stored privately as a small PNG. */
object LogoStore {
    private fun file(context: Context) = File(context.applicationContext.filesDir, "wiz_logo.png")

    fun exists(context: Context): Boolean = file(context).exists()

    fun load(context: Context): Bitmap? =
        file(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }

    fun save(context: Context, uri: Uri): Boolean {
        val bmp = ImageUtil.decodeSampled(context, uri, 256) ?: return false
        return try {
            FileOutputStream(file(context)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            false
        }
    }

    fun remove(context: Context) {
        file(context).delete()
    }
}
