package ro.andrei.ariciu.feature.places

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Searchable places: the Romania catalogue bundled in the app plus every country pack installed from Settings -> Search data
 * (files/places/<code>/). Queries never leave the device. An installed "ro" pack replaces the bundled Romania data.
 */
object LocalPlaces {
    @Volatile private var cached: PlaceIndex? = null

    fun index(context: Context): PlaceIndex = cached ?: synchronized(this) {
        cached ?: PlaceIndex { path -> open(context.applicationContext, path) }.also { cached = it }
    }

    /** Call after a pack was installed, updated or removed. */
    fun reload() { cached?.reset() }

    private fun open(context: Context, path: String): InputStream? {
        val dirs = File(context.filesDir, "places").listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") && File(it, "pack.txt").isFile }?.sortedBy { it.name } ?: emptyList()
        val streams = ArrayList<InputStream>()
        try {
            for (dir in dirs) File(dir, path).takeIf { it.isFile }?.let { streams += it.inputStream() }
            if (dirs.none { it.name == "ro" }) try { streams += context.assets.open("places/$path") } catch (_: IOException) { }
        } catch (e: IOException) { streams.forEach { try { it.close() } catch (_: IOException) { } }; throw e }
        return mergedStream(streams)
    }
}
