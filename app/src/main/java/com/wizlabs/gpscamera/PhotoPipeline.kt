package com.wizlabs.gpscamera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Takes the picture, then (off the main thread) fixes its rotation, draws the stamp, writes EXIF
 * (GPS, time, direction, integrity note), saves to Pictures/Wiz GPS Camera and records the
 * result in the tamper-evidence ledger.
 */
object PhotoPipeline {
    data class Saved(val uri: Uri, val name: String)

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** Call on the main thread. [onDone] is invoked on the main thread. */
    fun capture(
        context: Context,
        imageCapture: ImageCapture,
        data: StampData,
        settings: AppSettings,
        map: Bitmap?,
        logo: Bitmap?,
        project: Project?,
        /** null keeps CameraX's default (front camera photos are mirrored like the preview). */
        reverseHorizontal: Boolean?,
        onDone: (Result<Saved>) -> Unit
    ) {
        val app = context.applicationContext
        val raw = File(app.cacheDir, "wiz_raw_${System.nanoTime()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(raw)
        if (reverseHorizontal != null) {
            options.setMetadata(ImageCapture.Metadata().apply { isReversedHorizontal = reverseHorizontal })
        }
        imageCapture.takePicture(
            options.build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    raw.delete()
                    main.post { onDone(Result.failure(exception)) }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val result = runCatching { process(app, raw, data, settings, map, logo, project) }
                    raw.delete()
                    main.post { onDone(result) }
                }
            }
        )
    }

    private fun process(
        app: Context, raw: File, d: StampData, s: AppSettings, map: Bitmap?, logo: Bitmap?, project: Project?
    ): Saved {
        // 1) Decode and apply the rotation CameraX stored in EXIF (BitmapFactory ignores it).
        val exif = ExifInterface(raw.absolutePath)
        val rotation = exif.rotationDegrees
        val flipped = exif.isFlipped
        val opts = BitmapFactory.Options().apply {
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bmp = BitmapFactory.decodeFile(raw.absolutePath, opts)
            ?: throw IllegalStateException("Could not decode the captured photo")
        if (rotation != 0 || flipped) {
            val m = Matrix()
            if (rotation != 0) m.postRotate(rotation.toFloat())
            if (flipped) m.postScale(-1f, 1f)
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated !== bmp) bmp.recycle()
            bmp = if (rotated.isMutable) rotated else rotated.copy(Bitmap.Config.ARGB_8888, true).also { rotated.recycle() }
        }

        // 1b) 1:1 photos are cropped from the centre of the 4:3 capture.
        if (s.aspect == AspectChoice.R1_1) {
            val side = minOf(bmp.width, bmp.height)
            val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            if (square !== bmp) bmp.recycle()
            bmp = if (square.isMutable) square else square.copy(Bitmap.Config.ARGB_8888, true).also { square.recycle() }
        }

        // 2) Stamp it.
        StampRenderer.draw(Canvas(bmp), bmp.width, bmp.height, d, s, map, logo)

        // 3) Encode to a temp file, then add EXIF to that file.
        val stamped = File(app.cacheDir, "wiz_out_${System.nanoTime()}.jpg")
        try {
            FileOutputStream(stamped).use { out ->
                if (!bmp.compress(Bitmap.CompressFormat.JPEG, 98, out)) {
                    throw IllegalStateException("Image encoding failed")
                }
            }
            bmp.recycle()
            writeExif(stamped, d, s)

            // 4) Save to the gallery.
            val stampName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(d.timeMs))
            val name = "WizGPS_${stampName}_${d.photoId.takeLast(6)}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/Wiz GPS Camera" + (project?.let { "/" + ProjectStore.folderName(it.name) } ?: "")
                )
                put(MediaStore.Images.Media.DATE_TAKEN, d.timeMs)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = app.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Unable to create the photo in the gallery")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(stamped).use { it.copyTo(out) }
                } ?: throw IllegalStateException("Unable to write the photo")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }

            // 5) Record it in the tamper-evidence ledger (hash of the exact saved bytes).
            Integrity.add(
                app, d.photoId, name, Integrity.sha256(stamped),
                d.lat, d.lon, d.timeMs, d.mock, d.note
            )
            PhotoMetaStore.put(app, PhotoMetaStore.fromStamp(name, d, project?.id ?: ""))
            return Saved(uri, name)
        } finally {
            if (!bmp.isRecycled) bmp.recycle()
            stamped.delete()
        }
    }

    private fun writeExif(file: File, d: StampData, s: AppSettings) {
        val ex = ExifInterface(file.absolutePath)
        ex.setAttribute(ExifInterface.TAG_SOFTWARE, "Wiz GPS Camera")
        // Pixels were already rotated upright, so the orientation tag must say "normal".
        ex.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())

        val tz = StampFormat.zone(s)
        val local = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply { timeZone = tz }
        val stamp = local.format(Date(d.timeMs))
        val offset = StampFormat.offsetLabel(tz, d.timeMs)
        ex.setAttribute(ExifInterface.TAG_DATETIME, stamp)
        ex.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, stamp)
        ex.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, stamp)
        ex.setAttribute(ExifInterface.TAG_OFFSET_TIME, offset)
        ex.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, offset)

        val lat = d.lat
        val lon = d.lon
        if (lat != null && lon != null) {
            try {
                ex.setLatLong(lat, lon)
            } catch (e: IllegalArgumentException) {
                // out-of-range coordinates: skip GPS tags
            }
            d.altitudeM?.let { ex.setAltitude(it) }
            val utc = TimeZone.getTimeZone("UTC")
            val dateFmt = SimpleDateFormat("yyyy:MM:dd", Locale.US).apply { timeZone = utc }
            val hourFmt = SimpleDateFormat("HH", Locale.US).apply { timeZone = utc }
            val minFmt = SimpleDateFormat("mm", Locale.US).apply { timeZone = utc }
            val secFmt = SimpleDateFormat("ss", Locale.US).apply { timeZone = utc }
            val t = Date(d.timeMs)
            ex.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, dateFmt.format(t))
            ex.setAttribute(
                ExifInterface.TAG_GPS_TIMESTAMP,
                "${hourFmt.format(t).toInt()}/1,${minFmt.format(t).toInt()}/1,${secFmt.format(t).toInt()}/1"
            )
            d.headingDeg?.let {
                ex.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION, "${(it * 100).roundToInt()}/100")
                ex.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, if (d.headingTrue) "T" else "M")
            }
            d.speedKmh?.let {
                ex.setAttribute(ExifInterface.TAG_GPS_SPEED, "${(it * 100).roundToInt()}/100")
                ex.setAttribute(ExifInterface.TAG_GPS_SPEED_REF, "K")
            }
        }

        val description = listOfNotNull(
            d.note.takeIf { it.isNotBlank() },
            d.address?.replace("\n", ", ")
        ).joinToString(" | ")
        if (description.isNotBlank()) ex.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description)

        val timeTag = when (d.timeStatus) {
            TimeSync.Status.NETWORK -> "network"
            TimeSync.Status.ANCHORED -> "synced"
            TimeSync.Status.DEVICE -> "device"
        }
        ex.setAttribute(
            ExifInterface.TAG_USER_COMMENT,
            "WizGPS ID=${d.photoId} mock=${if (d.mock) 1 else 0} time=$timeTag" +
                (d.accuracyM?.let { " acc=${it.toInt()}m" } ?: "")
        )
        ex.saveAttributes()
    }
}
