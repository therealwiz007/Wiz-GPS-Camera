package com.wizlabs.gpscamera

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A job, site or album that photos are filed under. Photos also go into a matching sub-folder. */
data class Project(val id: String, val name: String, val createdMs: Long)

/** Keeps the list of projects. Observable so the camera and gallery update when it changes. */
class ProjectStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wiz_projects", Context.MODE_PRIVATE)

    var projects by mutableStateOf(load())
        private set

    fun byId(id: String): Project? = projects.firstOrNull { it.id == id }

    fun create(name: String): Project {
        val p = Project(UUID.randomUUID().toString().take(8), name.trim().take(60), System.currentTimeMillis())
        projects = projects + p
        save()
        return p
    }

    fun rename(id: String, name: String) {
        projects = projects.map { if (it.id == id) it.copy(name = name.trim().take(60)) else it }
        save()
    }

    /** Removes the project only; its photos stay in the gallery. */
    fun delete(id: String) {
        projects = projects.filterNot { it.id == id }
        save()
    }

    private fun load(): List<Project> = try {
        val arr = JSONArray(prefs.getString("list", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Project(o.getString("id"), o.getString("name"), o.optLong("created"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save() {
        val arr = JSONArray()
        projects.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("created", it.createdMs)) }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    companion object {
        /** Folder-safe version of a project name. */
        fun folderName(name: String): String =
            name.replace(Regex("[\\\\/:*?\"<>|]"), " ").replace(Regex("\\s+"), " ").trim().take(40).ifBlank { "Project" }
    }
}

/**
 * Everything printed on a photo, kept per file name so the gallery can filter, map and export
 * photos without re-reading EXIF. Stored privately as one JSON object per line.
 */
data class PhotoMeta(
    val name: String,
    val id: String,
    val projectId: String,
    val lat: Double?,
    val lon: Double?,
    val accuracyM: Float?,
    val altitudeM: Double?,
    val headingDeg: Float?,
    val speedKmh: Float?,
    val address: String?,
    val note: String,
    val timeMs: Long,
    val mock: Boolean,
    val weather: String?
)

object PhotoMetaStore {
    private val lock = Any()
    private var cache: LinkedHashMap<String, PhotoMeta>? = null

    private fun file(context: Context) = File(context.applicationContext.filesDir, "wiz_photo_meta.jsonl")

    fun fromStamp(name: String, d: StampData, projectId: String) = PhotoMeta(
        name = name, id = d.photoId, projectId = projectId, lat = d.lat, lon = d.lon,
        accuracyM = d.accuracyM, altitudeM = d.altitudeM, headingDeg = d.headingDeg, speedKmh = d.speedKmh,
        address = d.address, note = d.note, timeMs = d.timeMs, mock = d.mock, weather = d.weather
    )

    private fun toJson(m: PhotoMeta): String = JSONObject()
        .put("name", m.name).put("id", m.id).put("project", m.projectId)
        .put("lat", m.lat ?: JSONObject.NULL).put("lon", m.lon ?: JSONObject.NULL)
        .put("acc", m.accuracyM?.toDouble() ?: JSONObject.NULL)
        .put("alt", m.altitudeM ?: JSONObject.NULL)
        .put("hdg", m.headingDeg?.toDouble() ?: JSONObject.NULL)
        .put("spd", m.speedKmh?.toDouble() ?: JSONObject.NULL)
        .put("addr", m.address ?: JSONObject.NULL)
        .put("note", m.note).put("time", m.timeMs).put("mock", m.mock)
        .put("wx", m.weather ?: JSONObject.NULL)
        .toString()

    private fun fromJson(line: String): PhotoMeta? = try {
        val o = JSONObject(line)
        fun dOrNull(k: String) = if (o.isNull(k) || !o.has(k)) null else o.getDouble(k)
        fun sOrNull(k: String) = if (o.isNull(k) || !o.has(k)) null else o.getString(k)
        PhotoMeta(
            name = o.getString("name"), id = o.optString("id"), projectId = o.optString("project"),
            lat = dOrNull("lat"), lon = dOrNull("lon"), accuracyM = dOrNull("acc")?.toFloat(),
            altitudeM = dOrNull("alt"), headingDeg = dOrNull("hdg")?.toFloat(), speedKmh = dOrNull("spd")?.toFloat(),
            address = sOrNull("addr"), note = o.optString("note"), timeMs = o.optLong("time"),
            mock = o.optBoolean("mock"), weather = sOrNull("wx")
        )
    } catch (e: Exception) {
        null
    }

    private fun all(context: Context): LinkedHashMap<String, PhotoMeta> = synchronized(lock) {
        cache?.let { return@synchronized it }
        val map = LinkedHashMap<String, PhotoMeta>()
        val f = file(context)
        if (f.exists()) f.forEachLine { line -> fromJson(line)?.let { map[it.name] = it } }
        cache = map
        map
    }

    fun get(context: Context, name: String): PhotoMeta? = all(context)[name]

    fun put(context: Context, meta: PhotoMeta) = synchronized(lock) {
        file(context).appendText(toJson(meta) + "\n")
        all(context)[meta.name] = meta
    }

    /** Re-files photos under another project (the later line wins when the file is read back). */
    fun move(context: Context, names: Collection<String>, projectId: String) = synchronized(lock) {
        val map = all(context)
        val sb = StringBuilder()
        names.forEach { n ->
            map[n]?.let {
                val moved = it.copy(projectId = projectId)
                map[n] = moved
                sb.append(toJson(moved)).append('\n')
            }
        }
        if (sb.isNotEmpty()) file(context).appendText(sb.toString())
    }
}
