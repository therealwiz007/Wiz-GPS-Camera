package com.wizlabs.gpscamera

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Tamper evidence. After each photo is saved, its SHA-256 and capture details go into a private
 * ledger on the device, signed with an HMAC key held in the Android Keystore (the key cannot be
 * exported). The gallery re-hashes a photo and compares: any edit to the saved file shows up as
 * "Modified". This can only be checked inside this app on this device.
 */
object Integrity {
    enum class Result { VERIFIED, MODIFIED, UNKNOWN, LEDGER_TAMPERED }

    data class Record(
        val id: String,
        val name: String,
        val sha256: String,
        val hmac: String,
        val lat: Double?,
        val lon: Double?,
        val timeMs: Long,
        val mock: Boolean,
        val note: String
    )

    private const val ALIAS = "wiz_integrity_hmac"
    private val lock = Any()
    private var cached: List<Record>? = null

    private fun ledgerFile(context: Context) = File(context.applicationContext.filesDir, "wiz_ledger.jsonl")

    private fun key(): SecretKey? = try {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey) ?: run {
            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
            kg.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN).build())
            kg.generateKey()
        }
    } catch (e: Exception) {
        null
    }

    private fun payload(r: Record): String =
        listOf(r.id, r.name, r.sha256, r.lat?.toString() ?: "", r.lon?.toString() ?: "", r.timeMs.toString(), r.mock.toString(), r.note)
            .joinToString("|")

    private fun sign(text: String): String {
        val k = key() ?: return ""
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(k)
            hex(mac.doFinal(text.toByteArray(Charsets.UTF_8)))
        } catch (e: Exception) {
            ""
        }
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private fun digest(input: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            md.update(buf, 0, n)
        }
        return hex(md.digest())
    }

    fun sha256(file: File): String = FileInputStream(file).use { digest(it) }

    fun sha256(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { digest(it) }
    } catch (e: Exception) {
        null
    }

    /** Called right after a photo has been saved. [sha256] must be the hash of the saved file. */
    fun add(
        context: Context, id: String, name: String, sha256: String,
        lat: Double?, lon: Double?, timeMs: Long, mock: Boolean, note: String
    ) {
        val unsigned = Record(id, name, sha256, "", lat, lon, timeMs, mock, note)
        val rec = unsigned.copy(hmac = sign(payload(unsigned)))
        val line = JSONObject()
            .put("id", rec.id).put("name", rec.name).put("sha", rec.sha256).put("hmac", rec.hmac)
            .put("lat", rec.lat ?: JSONObject.NULL).put("lon", rec.lon ?: JSONObject.NULL)
            .put("time", rec.timeMs).put("mock", rec.mock).put("note", rec.note)
            .toString()
        synchronized(lock) {
            ledgerFile(context).appendText(line + "\n")
            cached = null
        }
    }

    private fun records(context: Context): List<Record> = synchronized(lock) {
        cached?.let { return@synchronized it }
        val out = ArrayList<Record>()
        val f = ledgerFile(context)
        if (f.exists()) {
            f.forEachLine { line ->
                try {
                    val o = JSONObject(line)
                    out.add(
                        Record(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            sha256 = o.getString("sha"),
                            hmac = o.optString("hmac", ""),
                            lat = if (o.isNull("lat")) null else o.getDouble("lat"),
                            lon = if (o.isNull("lon")) null else o.getDouble("lon"),
                            timeMs = o.getLong("time"),
                            mock = o.optBoolean("mock", false),
                            note = o.optString("note", "")
                        )
                    )
                } catch (e: Exception) {
                    // skip a damaged line
                }
            }
        }
        cached = out
        out
    }

    fun find(context: Context, displayName: String): Record? =
        records(context).lastOrNull { it.name == displayName }

    fun verify(context: Context, uri: Uri, displayName: String): Result {
        val sha = sha256(context, uri) ?: return Result.UNKNOWN
        val all = records(context)
        val rec = all.lastOrNull { it.name == displayName } ?: all.lastOrNull { it.sha256 == sha }
            ?: return Result.UNKNOWN
        if (rec.hmac.isNotEmpty() && sign(payload(rec.copy(hmac = ""))) != rec.hmac) return Result.LEDGER_TAMPERED
        return if (rec.sha256 == sha) Result.VERIFIED else Result.MODIFIED
    }
}
