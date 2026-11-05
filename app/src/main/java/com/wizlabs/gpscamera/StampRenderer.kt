package com.wizlabs.gpscamera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object StampFormat {
    fun zone(s: AppSettings): TimeZone =
        if (s.timeZoneId.isBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(s.timeZoneId)

    fun gmtLabel(tz: TimeZone, ms: Long): String {
        val off = tz.getOffset(ms)
        val sign = if (off < 0) "-" else "+"
        val m = abs(off) / 60000
        return String.format(Locale.US, "GMT%s%02d:%02d", sign, m / 60, m % 60)
    }

    fun offsetLabel(tz: TimeZone, ms: Long): String {
        val off = tz.getOffset(ms)
        val sign = if (off < 0) "-" else "+"
        val m = abs(off) / 60000
        return String.format(Locale.US, "%s%02d:%02d", sign, m / 60, m % 60)
    }

    /** e.g. "02 Nov 2026 • 02:45 PM" */
    fun dateTime(s: AppSettings, ms: Long): String {
        val tz = zone(s)
        val timePattern = when {
            s.clockFormat == ClockFormat.H12 && s.showSeconds -> "hh:mm:ss a"
            s.clockFormat == ClockFormat.H12 -> "hh:mm a"
            s.showSeconds -> "HH:mm:ss"
            else -> "HH:mm"
        }
        val f = SimpleDateFormat("${s.dateFormat} • $timePattern", Locale.getDefault())
        f.timeZone = tz
        var text = f.format(Date(ms))
            .replace(' ', ' ')
            .replace(' ', ' ')
            .replace(" am", " AM")
            .replace(" pm", " PM")
        if (s.showTimeZone) text += " " + gmtLabel(tz, ms)
        return text
    }

    fun heading(deg: Float): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val i = ((deg + 22.5f) / 45f).toInt() % 8
        return "${deg.toInt()}° ${dirs[i]}"
    }

    fun deviation(ms: Long): String {
        val a = abs(ms) / 1000
        return when {
            a >= 86400 -> "${a / 86400} d"
            a >= 3600 -> "${a / 3600} h ${(a % 3600) / 60} min"
            a >= 60 -> "${a / 60} min"
            else -> "$a s"
        }
    }
}

/**
 * Draws the geo-tag stamp: a frosted-glass card with the map on the left, a thin divider, then
 * place title, street line, coordinates and date/time. The same code draws the live preview
 * overlay and the saved photo, so what you see is what you get. Sizes are relative to the shorter
 * image side, so the stamp has the same proportions in portrait and landscape.
 */
object StampRenderer {
    private val TEAL = 0xFF70DCE8.toInt()
    private val GLASS = 0xB32B2F33.toInt()

    private class Line(val text: String, val paint: TextPaint, val maxLines: Int, var spaceBefore: Float)

    private fun paint(color: Int, size: Float, face: String, shadow: Boolean, scale: Float): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(face, Typeface.NORMAL)
            if (shadow) setShadowLayer(4f * scale, 0f, 2f * scale, 0xCC000000.toInt())
        }

    private fun layout(text: String, paint: TextPaint, width: Int, maxLines: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, max(width, 1))
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .build()

    fun draw(canvas: Canvas, w: Int, h: Int, d: StampData, s: AppSettings, map: Bitmap?, logo: Bitmap?) {
        val ref = min(w, h).toFloat()
        val scale = ref / 1080f
        val margin = 24f * scale
        val compact = s.template == StampTemplate.COMPACT
        val textOnly = s.template == StampTemplate.TEXT_ONLY
        val withMap = s.template == StampTemplate.FULL && s.showMap
        val left = s.position == StampPosition.LEFT
        val f = if (compact) 0.85f else 1f
        val pad = (if (compact) 14f else 24f) * scale
        val gap = 16f * scale
        val shadow = textOnly

        val titlePaint = paint(Color.WHITE, 38f * scale * f, "sans-serif-medium", shadow, scale)
        val bodyPaint = paint(0xFFEDEFF2.toInt(), 31f * scale * f, "sans-serif", shadow, scale)
        val notePaint = paint(TEAL, 31f * scale * f, "sans-serif-medium", shadow, scale)
        val smallPaint = paint(0xFFD7DCE1.toInt(), 26f * scale * f, "sans-serif", shadow, scale)
        val accent = paint(TEAL, 26f * scale * f, "sans-serif-medium", shadow, scale)
        val warn = paint(0xFFFFB74D.toInt(), 26f * scale * f, "sans-serif-bold", shadow, scale)

        // ---- Build the text lines from the enabled fields -------------------------------------
        val sp = 10f * scale * f
        val sp2 = 7f * scale * f
        val lines = ArrayList<Line>()
        val lat = d.lat
        val lon = d.lon
        val hasFix = lat != null && lon != null

        if (s.showNote && d.note.isNotBlank()) lines.add(Line(d.note, notePaint, 2, sp))

        if (s.showAddress) {
            val addr = d.address
            val title: String
            var street: String? = null
            when {
                !hasFix -> title = "Location unavailable"
                addr == null -> title = "Address unavailable"
                else -> {
                    val parts = addr.split('\n', limit = 2)
                    title = parts[0] + if (d.addressCached) " (approx.)" else ""
                    street = parts.getOrNull(1)
                }
            }
            lines.add(Line(title, titlePaint, 2, sp))
            if (!street.isNullOrBlank()) lines.add(Line(street, bodyPaint, 2, sp2))
        }
        // If the address could not be resolved, always fall back to showing the coordinates.
        val forceCoords = s.showAddress && hasFix && d.address == null
        if (s.showCoords || forceCoords) {
            val text = if (lat != null && lon != null) String.format(Locale.US, "Lat %.6f°  Long %.6f°", lat, lon)
            else "Coordinates unavailable"
            lines.add(Line(text, bodyPaint, 1, sp2))
        }
        if (s.showDateTime) lines.add(Line(StampFormat.dateTime(s, d.timeMs), bodyPaint, 1, sp2))
        if (s.showPlusCode && d.plusCode != null) lines.add(Line("Plus code  ${d.plusCode}", smallPaint, 1, sp2))

        val motion = ArrayList<String>()
        if (s.showAltitude) d.altitudeM?.let { motion.add("Alt ${it.toInt()} m") }
        if (s.showHeading) d.headingDeg?.let { motion.add("Hdg ${StampFormat.heading(it)}") }
        if (s.showSpeed) d.speedKmh?.let { motion.add("Speed ${it.toInt()} km/h") }
        if (motion.isNotEmpty()) lines.add(Line(motion.joinToString("  ·  "), smallPaint, 2, sp2))

        if (s.showAccuracy) {
            val acc = d.accuracyM
            val text = if (acc != null) {
                val age = d.fixAgeS ?: 0
                "GPS accuracy ±${acc.toInt()} m" + (if (age > 15) " · fix ${age}s old" else "")
            } else "GPS location unavailable"
            lines.add(Line(text, accent, 1, sp2))
        }
        if (s.showWeather && d.weather != null) lines.add(Line(d.weather, smallPaint, 1, sp2))

        // Integrity warnings are always printed, whatever the toggles say.
        if (d.mock) lines.add(Line("⚠ MOCK LOCATION DETECTED", warn, 1, sp))
        if (d.timeStatus != TimeSync.Status.DEVICE && abs(d.clockDeviationMs) > 60_000L) {
            lines.add(Line("⚠ Device clock off by ${StampFormat.deviation(d.clockDeviationMs)}", warn, 1, sp2))
        }
        if (s.showVerification) {
            val t = when (d.timeStatus) {
                TimeSync.Status.NETWORK -> "Time ✓ network"
                TimeSync.Status.ANCHORED -> "Time ✓ synced"
                TimeSync.Status.DEVICE -> "Time: device clock"
            }
            lines.add(Line("ID ${d.photoId}  ·  $t", smallPaint, 1, sp2))
        }

        // ---- Measure -------------------------------------------------------------------------
        val panelW = if (left) ref * 0.50f else min(w * 0.92f, ref * 0.92f)
        val innerW = panelW - 2 * pad
        val mapSide = 240f * scale * (if (compact) 0.85f else 1f)
        val divGap = 22f * scale
        val divW = 2f * scale
        val horizontalMap = withMap && !left
        val verticalMap = withMap && left
        val textOffset = if (horizontalMap) mapSide + 2 * divGap + divW else 0f
        val textW = innerW - textOffset
        val showLogoNow = logo != null && s.showLogo
        val logoSize = if (showLogoNow) 44f * scale else 0f
        val headerH = logoSize

        if (headerH == 0f) lines.firstOrNull()?.spaceBefore = 0f
        val layouts = lines.map { layout(it.text, it.paint, textW.toInt(), it.maxLines) }
        var textH = headerH
        lines.forEachIndexed { i, l -> textH += l.spaceBefore + layouts[i].height }

        val mapH = when {
            verticalMap -> innerW * 0.7f
            horizontalMap -> mapSide
            else -> 0f
        }
        val contentH = if (verticalMap) mapH + gap + textH else max(mapH, textH)
        val panelH = contentH + 2 * pad

        val pl = if (left) margin else (w - panelW) / 2f
        val pt = if (s.position == StampPosition.TOP) margin else h - panelH - margin
        val radius = 30f * scale

        // ---- Panel ---------------------------------------------------------------------------
        if (!textOnly) {
            val rect = RectF(pl, pt, pl + panelW, pt + panelH)
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x40000000
                setShadowLayer(14f * scale, 0f, 5f * scale, 0x66000000)
            }
            canvas.drawRoundRect(rect, radius, radius, shadowPaint)
            canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GLASS })
            canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x4DFFFFFF
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * scale
            })
        }

        // ---- Map and divider -----------------------------------------------------------------
        if (withMap) {
            val mapRect = if (verticalMap) {
                RectF(pl + pad, pt + pad, pl + pad + innerW, pt + pad + mapH)
            } else {
                val top = pt + pad + (contentH - mapH) / 2f
                RectF(pl + pad, top, pl + pad + mapSide, top + mapH)
            }
            drawMap(canvas, mapRect, map, scale)
            if (horizontalMap) {
                val x = pl + pad + mapSide + divGap + divW / 2f
                val y0 = pt + pad + contentH * 0.08f
                val y1 = pt + pad + contentH * 0.92f
                canvas.drawLine(x, y0, x, y1, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x47FFFFFF
                    strokeWidth = divW
                })
            }
        }

        // ---- Text ----------------------------------------------------------------------------
        val tx = pl + pad + textOffset
        val ty = when {
            verticalMap -> pt + pad + mapH + gap
            horizontalMap -> pt + pad + (contentH - textH) / 2f
            else -> pt + pad
        }

        if (showLogoNow && logo != null) {
            val ratio = logo.width.toFloat() / max(logo.height, 1)
            val lh = if (ratio >= 1f) logoSize / ratio else logoSize
            val lw = if (ratio >= 1f) logoSize else logoSize * ratio
            val right = tx + textW
            val dst = RectF(right - lw, ty, right, ty + lh)
            canvas.drawBitmap(logo, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }

        var y = ty + headerH
        lines.forEachIndexed { i, l ->
            y += l.spaceBefore
            canvas.save()
            canvas.translate(tx, y)
            layouts[i].draw(canvas)
            canvas.restore()
            y += layouts[i].height
        }

        // ---- Watermark -----------------------------------------------------------------------
        if (s.watermark.isNotBlank()) {
            val wm = paint(0xB0FFFFFF.toInt(), 22f * scale, "sans-serif-medium", true, scale)
            wm.textAlign = Paint.Align.RIGHT
            val baseline = if (s.position == StampPosition.TOP) h - margin else margin + wm.textSize
            canvas.drawText(s.watermark, w - margin, baseline, wm)
        }
    }

    private fun drawMap(canvas: Canvas, rect: RectF, map: Bitmap?, scale: Float) {
        val r = 20f * scale
        canvas.save()
        val path = Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) }
        canvas.clipPath(path)
        if (map != null) {
            // Centre-crop the snapshot into the target rectangle.
            val srcRatio = map.width.toFloat() / max(map.height, 1)
            val dstRatio = rect.width() / rect.height()
            val src = Rect(0, 0, map.width, map.height)
            if (srcRatio > dstRatio) {
                val newW = (map.height * dstRatio).toInt()
                src.left = (map.width - newW) / 2
                src.right = src.left + newW
            } else {
                val newH = (map.width / dstRatio).toInt()
                src.top = (map.height - newH) / 2
                src.bottom = src.top + newH
            }
            canvas.drawBitmap(map, src, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF233D49.toInt() })
            val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x5570DCE8
                strokeWidth = 2f * scale
            }
            for (i in 1..3) {
                val x = rect.left + rect.width() * i / 4f
                val yy = rect.top + rect.height() * i / 4f
                canvas.drawLine(x, rect.top, x, rect.bottom, grid)
                canvas.drawLine(rect.left, yy, rect.right, yy, grid)
            }
        }
        canvas.restore()
        drawPin(canvas, rect.centerX(), rect.centerY(), scale)
    }

    /** Red map pin whose tip sits exactly at (x, y). */
    private fun drawPin(canvas: Canvas, x: Float, y: Float, scale: Float) {
        val r = 17f * scale
        val headY = y - r * 2.1f
        val body = Path().apply {
            addCircle(x, headY, r, Path.Direction.CW)
            moveTo(x - r * 0.92f, headY + r * 0.42f)
            lineTo(x + r * 0.92f, headY + r * 0.42f)
            lineTo(x, y)
            close()
        }
        canvas.drawPath(body, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE53935.toInt()
            setShadowLayer(4f * scale, 0f, 2f * scale, 0x66000000)
        })
        canvas.drawCircle(x, headY, r * 0.42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8E1B1B.toInt() })
    }
}
