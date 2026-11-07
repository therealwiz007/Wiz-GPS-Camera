package com.wizlabs.gpscamera

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports a set of photos as a PDF report, a CSV table or a KML file (opens in Google Earth/Maps).
 * Files are written to Downloads/Wiz GPS Camera and returned as content URIs ready to share.
 */
object Export {
    enum class Kind(val label: String, val ext: String, val mime: String) {
        PDF("PDF report", "pdf", "application/pdf"),
        CSV("CSV spreadsheet", "csv", "text/csv"),
        KML("KML (Google Earth)", "kml", "application/vnd.google-earth.kml+xml")
    }

    /** A photo plus whatever we know about it. [meta] is null for photos with no record. */
    data class Entry(val item: PhotoItem, val meta: PhotoMeta?, val verified: Integrity.Result?)

    fun metaFor(context: Context, item: PhotoItem): PhotoMeta? =
        PhotoMetaStore.get(context, item.name) ?: Integrity.find(context, item.name)?.let { r ->
            PhotoMeta(
                name = r.name, id = r.id, projectId = "", lat = r.lat, lon = r.lon, accuracyM = null,
                altitudeM = null, headingDeg = null, speedKmh = null, address = null, note = r.note,
                timeMs = r.timeMs, mock = r.mock, weather = null
            )
        }

    /** Runs on a background thread. Returns the saved file's URI and name. */
    fun write(context: Context, kind: Kind, title: String, entries: List<Entry>, s: AppSettings): Pair<Uri, String> {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val safeTitle = ProjectStore.folderName(title).replace(' ', '_')
        val name = "WizGPS_${safeTitle}_$stamp.${kind.ext}"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, kind.mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/Wiz GPS Camera")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create $name")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                when (kind) {
                    Kind.PDF -> pdf(context, out, title, entries, s)
                    Kind.CSV -> out.write(csv(entries, s).toByteArray(Charsets.UTF_8))
                    Kind.KML -> out.write(kml(title, entries).toByteArray(Charsets.UTF_8))
                }
            } ?: throw IllegalStateException("Could not write $name")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri to name
    }

    fun share(context: Context, uri: Uri, kind: Kind, fileName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = kind.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, fileName)
            // Lets the share sheet show the real file name instead of the media ID.
            clipData = android.content.ClipData.newUri(context.contentResolver, fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${kind.label}"))
    }

    // ---- CSV -----------------------------------------------------------------------------------

    private fun cell(v: Any?): String {
        val t = v?.toString() ?: ""
        return if (t.any { it == ',' || it == '"' || it == '\n' }) "\"" + t.replace("\"", "\"\"") + "\"" else t
    }

    fun csv(entries: List<Entry>, s: AppSettings): String {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { timeZone = StampFormat.zone(s) }
        val sb = StringBuilder()
        sb.append("File,Photo ID,Date time,Latitude,Longitude,Accuracy m,Altitude m,Heading deg,Speed km/h,Address,Note,Mock location,Integrity\n")
        entries.forEach { e ->
            val m = e.meta
            sb.append(
                listOf(
                    e.item.name, m?.id, iso.format(Date(m?.timeMs ?: e.item.dateMs)),
                    m?.lat?.let { String.format(Locale.US, "%.7f", it) },
                    m?.lon?.let { String.format(Locale.US, "%.7f", it) },
                    m?.accuracyM?.toInt(), m?.altitudeM?.toInt(), m?.headingDeg?.toInt(), m?.speedKmh?.toInt(),
                    m?.address?.replace('\n', ' '), m?.note, if (m?.mock == true) "yes" else "no",
                    verdictShort(e.verified)
                ).joinToString(",") { cell(it) }
            ).append('\n')
        }
        return sb.toString()
    }

    // ---- KML -----------------------------------------------------------------------------------

    private fun xml(t: String?) = (t ?: "")
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    fun kml(title: String, entries: List<Entry>): String {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>\n")
        sb.append("<name>").append(xml(title)).append("</name>\n")
        entries.forEach { e ->
            val m = e.meta ?: return@forEach
            val lat = m.lat ?: return@forEach
            val lon = m.lon ?: return@forEach
            val desc = listOfNotNull(
                m.address?.replace("\n", ", "),
                m.note.takeIf { it.isNotBlank() },
                m.accuracyM?.let { "Accuracy ±${it.toInt()} m" },
                "File ${e.item.name}"
            ).joinToString("\n")
            sb.append("<Placemark><name>").append(xml(m.note.ifBlank { m.id.ifBlank { e.item.name } })).append("</name>")
            sb.append("<description>").append(xml(desc)).append("</description>")
            sb.append("<TimeStamp><when>").append(iso.format(Date(m.timeMs))).append("</when></TimeStamp>")
            sb.append(String.format(Locale.US, "<Point><coordinates>%.7f,%.7f", lon, lat))
            m.altitudeM?.let { sb.append(String.format(Locale.US, ",%.1f", it)) }
            sb.append("</coordinates></Point></Placemark>\n")
        }
        sb.append("</Document></kml>\n")
        return sb.toString()
    }

    // ---- PDF -----------------------------------------------------------------------------------

    private fun verdictShort(r: Integrity.Result?) = when (r) {
        Integrity.Result.VERIFIED -> "verified"
        Integrity.Result.MODIFIED -> "modified"
        Integrity.Result.LEDGER_TAMPERED -> "record tampered"
        Integrity.Result.UNKNOWN, null -> "unknown"
    }

    private const val PAGE_W = 595 // A4 at 72 dpi
    private const val PAGE_H = 842
    private const val M = 36f

    private fun tp(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun drawText(c: Canvas, text: String, paint: TextPaint, x: Float, y: Float, width: Float, maxLines: Int): Float {
        val l = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
            .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false).build()
        c.save()
        c.translate(x, y)
        l.draw(c)
        c.restore()
        return l.height.toFloat()
    }

    private fun pdf(context: Context, out: OutputStream, title: String, entries: List<Entry>, s: AppSettings) {
        val doc = PdfDocument()
        val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).apply { timeZone = StampFormat.zone(s) }
        val navy = 0xFF091923.toInt()
        val teal = 0xFF2BB3C4.toInt()
        val grey = 0xFF5F6B73.toInt()
        val titleP = tp(20f, navy, true)
        val subP = tp(10f, grey)
        val headP = tp(11f, navy, true)
        val bodyP = tp(9f, 0xFF222A30.toInt())
        val labelP = tp(8f, grey, true)
        val warnP = tp(9f, 0xFFD84315.toInt(), true)
        val okP = tp(9f, 0xFF2E7D32.toInt(), true)
        var pageNo = 0
        val perPage = 2
        val pages = ((entries.size + perPage - 1) / perPage).coerceAtLeast(1)

        fun footer(c: Canvas) {
            val p = tp(8f, grey)
            c.drawText("Wiz GPS Camera · Generated ${dateFmt.format(Date())}", M, PAGE_H - 20f, p)
            p.textAlign = Paint.Align.RIGHT
            c.drawText("Page $pageNo of $pages", PAGE_W - M, PAGE_H - 20f, p)
        }

        // Summary header goes on the first page above the first photos.
        entries.chunked(perPage).ifEmpty { listOf(emptyList()) }.forEach { chunk ->
            pageNo++
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            val c = page.canvas
            c.drawColor(Color.WHITE)
            var y = M
            if (pageNo == 1) {
                c.drawRect(0f, 0f, PAGE_W.toFloat(), 6f, Paint().apply { color = teal })
                y += drawText(c, title, titleP, M, y, PAGE_W - 2 * M, 2) + 4f
                val times = entries.mapNotNull { it.meta?.timeMs ?: it.item.dateMs }
                val range = if (times.isEmpty()) "" else
                    " · " + dateFmt.format(Date(times.min())) + " – " + dateFmt.format(Date(times.max()))
                val verified = entries.count { it.verified == Integrity.Result.VERIFIED }
                y += drawText(
                    c, "${entries.size} photo${if (entries.size == 1) "" else "s"}$range · $verified verified original${if (verified == 1) "" else "s"}",
                    subP, M, y, PAGE_W - 2 * M, 2
                ) + 14f
            }
            val slotH = (PAGE_H - y - 40f) / perPage
            chunk.forEach { e ->
                drawEntry(context, c, e, M, y, PAGE_W - 2 * M, slotH - 12f, s, dateFmt, headP, bodyP, labelP, warnP, okP)
                y += slotH
            }
            footer(c)
            doc.finishPage(page)
        }
        doc.writeTo(out)
        doc.close()
    }

    private fun drawEntry(
        context: Context, c: Canvas, e: Entry, x: Float, y: Float, w: Float, h: Float, s: AppSettings,
        dateFmt: SimpleDateFormat, headP: TextPaint, bodyP: TextPaint, labelP: TextPaint, warnP: TextPaint, okP: TextPaint
    ) {
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = 0xFFD5DBE0.toInt()
        }
        c.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, border)
        val pad = 10f
        val imgMaxW = w * 0.58f
        val imgMaxH = h - 2 * pad
        // Sample big enough to look sharp when printed (~150 dpi at this size).
        val bmp = ImageUtil.decodeSampled(context, e.item.uri, 1400)
        var imgW = imgMaxW
        if (bmp != null) {
            val ratio = bmp.width.toFloat() / bmp.height
            var dw = imgMaxW
            var dh = dw / ratio
            if (dh > imgMaxH) {
                dh = imgMaxH
                dw = dh * ratio
            }
            imgW = dw
            val top = y + pad + (imgMaxH - dh) / 2f
            c.drawBitmap(bmp, null, RectF(x + pad, top, x + pad + dw, top + dh), Paint(Paint.FILTER_BITMAP_FLAG))
            bmp.recycle()
        }
        val tx = x + pad + imgW + 14f
        val tw = x + w - pad - tx
        var ty = y + pad
        val m = e.meta
        ty += drawText(c, m?.note?.takeIf { it.isNotBlank() } ?: e.item.name, headP, tx, ty, tw, 2) + 6f

        fun field(label: String, value: String?, paint: TextPaint = bodyP, lines: Int = 3) {
            if (value.isNullOrBlank()) return
            ty += drawText(c, label.uppercase(Locale.US), labelP, tx, ty, tw, 1) + 1f
            ty += drawText(c, value, paint, tx, ty, tw, lines) + 6f
        }
        field("Date and time", dateFmt.format(Date(m?.timeMs ?: e.item.dateMs)))
        field("Address", m?.address?.replace("\n", ", "), lines = 4)
        if (m?.lat != null && m.lon != null) {
            field("Coordinates", Geo.format(m.lat, m.lon, s.coordFormat) +
                if (s.coordFormat != CoordFormat.DECIMAL) "\n" + String.format(Locale.US, "%.6f, %.6f", m.lat, m.lon) else "")
        }
        val extra = listOfNotNull(
            m?.accuracyM?.let { "±${Geo.distance(it.toDouble(), s.units)}" },
            m?.altitudeM?.let { "Alt ${Geo.distance(it, s.units)}" },
            m?.headingDeg?.let { "Hdg ${StampFormat.heading(it)}" }
        ).joinToString(" · ")
        field("GPS", extra.ifBlank { null })
        field("Weather", m?.weather)
        field("Photo ID", m?.id?.ifBlank { null })
        if (m?.mock == true) field("Warning", "Mock location detected", warnP)
        val verdict = when (e.verified) {
            Integrity.Result.VERIFIED -> "✓ Unmodified original" to okP
            Integrity.Result.MODIFIED -> "File was modified after capture" to warnP
            Integrity.Result.LEDGER_TAMPERED -> "Integrity record failed check" to warnP
            else -> null
        }
        verdict?.let { field("Integrity", it.first, it.second, 2) }
    }
}
