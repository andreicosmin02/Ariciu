package ro.andrei.arichu.feature.places

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import ro.andrei.arichu.feature.offlinemap.STORAGE_HEADROOM_BYTES
import ro.andrei.arichu.feature.offlinemap.hasRoomFor
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/** A pack being installed: [label] is what the screen shows ("Downloading", "Unpacking"), [progress] is 0..1. */
data class PackWork(val code: String, val label: String, val progress: Float)

/**
 * Downloads, unpacks, updates and removes the "Search data" packs. Network is used only when the user taps a button in
 * Settings, and only for the release files on github.com (index.tsv and places-<code>.zip, public, no account).
 * Packs live in files/places/<code>/ (towns.tsv, grid/...) next to a pack.txt holding the zip's hash.
 */
class PlacePackManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "places")
    private val indexCache = File(appContext.filesDir, "places-index.tsv")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    private val mutableCatalog = MutableStateFlow(readCachedCatalog())
    private val mutableInstalled = MutableStateFlow(readInstalled())
    private val mutableWork = MutableStateFlow<PackWork?>(null)
    private val mutableMessage = MutableStateFlow<String?>(null)
    private val mutableRefreshing = MutableStateFlow(false)
    val catalog: StateFlow<List<PlacePack>> = mutableCatalog.asStateFlow()
    val installed: StateFlow<Map<String, InstalledPack>> = mutableInstalled.asStateFlow()
    val work: StateFlow<PackWork?> = mutableWork.asStateFlow()
    val message: StateFlow<String?> = mutableMessage.asStateFlow()
    val refreshing: StateFlow<Boolean> = mutableRefreshing.asStateFlow()

    init {
        // Half-finished installs from a killed process.
        root.listFiles()?.filter { it.name.startsWith(".") }?.forEach { it.deleteRecursively() }
        appContext.cacheDir.listFiles()?.filter { it.name.startsWith("places-") && it.name.endsWith(".part") }?.forEach { it.delete() }
    }

    private fun readCachedCatalog(): List<PlacePack> = try { if (indexCache.isFile) parsePackIndex(indexCache.readText()) else emptyList() } catch (_: IOException) { emptyList() }

    private fun readInstalled(): Map<String, InstalledPack> = root.listFiles()?.mapNotNull { dir ->
        if (!dir.isDirectory || dir.name.startsWith(".")) return@mapNotNull null
        try {
            val f = File(dir, "pack.txt").readText().trim().split('\t')
            if (f.size >= 2 && f[0].length == 64) InstalledPack(dir.name, f[0], f[1]) else null
        } catch (_: IOException) { null }
    }?.associateBy { it.code } ?: emptyMap()

    private fun freeBytes(): Long = try { android.os.StatFs(appContext.filesDir.path).availableBytes } catch (_: Exception) { Long.MAX_VALUE }

    fun dismissMessage() { mutableMessage.value = null }

    /** Fetches the list of available packs (tiny file). */
    fun refresh() {
        if (mutableRefreshing.value) return
        mutableRefreshing.value = true; mutableMessage.value = null
        scope.launch {
            try {
                val text = http.newCall(request(INDEX_URL)).execute().use { r ->
                    if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
                    val body = r.body ?: throw IOException("empty")
                    if (body.contentLength() > 1_000_000) throw IOException("too large")
                    body.string()
                }
                val packs = parsePackIndex(text)
                if (packs.isEmpty()) throw IOException("empty list")
                indexCache.writeText(text)
                mutableCatalog.value = packs
            } catch (e: CancellationException) { throw e } catch (_: Exception) {
                mutableMessage.value = "Could not load the list. Check your internet connection and try again."
            } finally { mutableRefreshing.value = false }
        }
    }

    /** Installs or updates [packs] one after the other. Does nothing while another install runs. */
    fun install(packs: List<PlacePack>) {
        if (packs.isEmpty() || job?.isActive == true) return
        mutableMessage.value = null
        job = scope.launch {
            try {
                for (pack in packs) installOne(pack)
            } catch (e: CancellationException) { throw e } catch (e: UserFacing) { mutableMessage.value = e.message } catch (e: Exception) {
                android.util.Log.w("ArichuPlaces", "pack install failed", e)
                mutableMessage.value = "The download did not finish. Check your internet connection and try again."
            } finally { mutableWork.value = null }
        }
    }

    fun cancel() { job?.cancel() }

    fun remove(code: String) {
        if (job?.isActive == true) return
        scope.launch {
            File(root, code).takeIf { it.isDirectory }?.deleteRecursively()
            mutableInstalled.value = readInstalled()
            LocalPlaces.reload()
        }
    }

    private class UserFacing(message: String) : Exception(message)

    private fun request(url: String): Request {
        require(url.startsWith("https://")) { "https only" }
        return Request.Builder().url(url).header("User-Agent", "Arichu-Android/1.1 (ro.andrei.arichu; search data)").build()
    }

    private suspend fun installOne(pack: PlacePack) {
        if (!hasRoomFor(freeBytes(), pack.bytes)) throw UserFacing("Not enough free storage for ${pack.name}. Free some space and try again.")
        val part = File(appContext.cacheDir, "places-${pack.code}.zip.part")
        val staging = File(root, ".new-${pack.code}")
        try {
            mutableWork.value = PackWork(pack.code, "Downloading", 0f)
            val digest = MessageDigest.getInstance("SHA-256")
            var received = 0L
            http.newCall(request(BASE_URL + pack.file)).execute().use { r ->
                if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
                val body = r.body ?: throw IOException("empty")
                body.byteStream().use { input -> part.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer); if (n < 0) break
                        received += n
                        if (received > pack.bytes) throw UserFacing("The file for ${pack.name} is larger than expected, so it was not installed.")
                        digest.update(buffer, 0, n); out.write(buffer, 0, n)
                        mutableWork.value = PackWork(pack.code, "Downloading", (received.toFloat() / pack.bytes).coerceIn(0f, 1f) * 0.8f)
                        if (!kotlin.coroutines.coroutineContext.isActive) throw CancellationException()
                    }
                } }
            }
            if (received != pack.bytes || digest.digest().joinToString("") { "%02x".format(it) } != pack.sha256)
                throw UserFacing("The file for ${pack.name} did not match its checksum, so it was not installed. Try again later.")

            mutableWork.value = PackWork(pack.code, "Unpacking", 0.8f)
            val unpacked = ZipFile(part).use { z -> z.entries().asSequence().sumOf { it.size.coerceAtLeast(0) } }
            if (!hasRoomFor(freeBytes(), unpacked)) throw UserFacing("Not enough free storage for ${pack.name} (needs about ${unpacked / 1_000_000 + STORAGE_HEADROOM_BYTES / 1_000_000} MB). Free some space and try again.")
            staging.deleteRecursively(); staging.mkdirs()
            val names = HashSet<String>()
            part.inputStream().buffered().use { input ->
                unpackPack(input, maxBytes = unpacked + 1_000_000) { name, data ->
                    val target = File(staging, name)
                    target.parentFile?.mkdirs()
                    target.outputStream().use { data.copyTo(it) }
                    names += name
                }
            }
            if (!hasRequiredEntries(names)) throw PackFormatException("The pack is incomplete")
            File(staging, "pack.txt").writeText("${pack.sha256}\t${pack.generated}\n")
            val target = File(root, pack.code); val old = File(root, ".old-${pack.code}")
            old.deleteRecursively()
            if (target.exists() && !target.renameTo(old)) throw IOException("could not replace the old pack")
            if (!staging.renameTo(target)) { old.renameTo(target); throw IOException("could not install the pack") }
            old.deleteRecursively()
            mutableInstalled.value = readInstalled()
            LocalPlaces.reload()
        } catch (e: PackFormatException) {
            throw UserFacing("The file for ${pack.name} could not be read, so it was not installed.")
        } catch (e: IOException) {
            if (storageRanOut(e)) throw UserFacing("The phone ran out of storage while installing ${pack.name}. Free some space and try again.")
            throw e
        } finally {
            part.delete(); staging.deleteRecursively()
        }
    }

    private fun storageRanOut(e: IOException) = (e.message ?: "").contains("ENOSPC") || (e.message ?: "").contains("No space left") || freeBytes() < STORAGE_HEADROOM_BYTES / 4

    companion object {
        const val BASE_URL = "https://github.com/andreicosmin02/Arichu/releases/download/places-data/"
        const val INDEX_URL = BASE_URL + "index.tsv"
        @Volatile private var instance: PlacePackManager? = null
        fun get(context: Context): PlacePackManager = instance ?: synchronized(this) { instance ?: PlacePackManager(context).also { instance = it } }
    }
}
