package ro.andrei.ariciu.feature.offlinemap

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.*
import org.maplibre.android.module.http.HttpRequestUtil
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import ro.andrei.ariciu.feature.places.GeoBounds
import kotlin.math.cos

/** One explicitly requested pack at a time, all native requests share the provider limits. */
object MapNetwork {
    private var installed = false
    private var downloading = false
    private var budget = DownloadBudget()
    @Synchronized fun begin(existingBytes: Long) { downloading = true; budget = DownloadBudget(existingBytes) }
    @Synchronized fun end() { downloading = false }
    @Synchronized private fun count(n: Int, tile: Boolean = false) {
        if (!downloading) return
        budget.consume(n, tile)
    }
    @Synchronized fun install() {
        if (installed) return
        val dispatcher = Dispatcher().apply { maxRequests = 4; maxRequestsPerHost = 4 }
        HttpRequestUtil.setOkHttpClient(OkHttpClient.Builder().dispatcher(dispatcher).addInterceptor { chain ->
            val request = chain.request().newBuilder().header("User-Agent", "Ariciu-Android/1.1 (ro.andrei.ariciu; limited offline maps)").build()
            val response = chain.proceed(request)
            val body = response.body
            if (body == null) response else {
                try {
                    count(0, request.url.encodedPath.endsWith(".pbf"))
                    val output = ByteArrayOutputStream()
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        while (true) { val n = input.read(buffer); if (n < 0) break; count(n); output.write(buffer, 0, n) }
                    }
                    response.newBuilder().body(output.toByteArray().toResponseBody(body.contentType())).build()
                } catch (e: Exception) { response.close(); throw e }
            }
        }.build())
        installed = true
    }
}

data class OfflineArea(val id: Long, val name: String, val bytes: Long = 0, val complete: Boolean = false,
    val progress: Float = 0f, val active: Boolean = false, val error: String? = null,
    val bounds: GeoBounds? = null, val dark: Boolean? = null, val minZoom: Int = OfflineLimits.MIN_ZOOM, val maxZoom: Int = OfflineLimits.MAX_ZOOM,
    val tiles: Long = 0, val tileBytes: Long = 0, val limitReached: Boolean = false) {
    fun toRegion(): DownloadedRegion? = if (bounds != null && dark != null) DownloadedRegion(bounds, dark, minZoom, maxZoom, complete) else null
    fun toStats() = PackStats(complete, tiles, tileBytes, bytes, limitReached)
}

class OfflineMaps private constructor(context: Context) {
    private val appContext = context.applicationContext
    private fun freeBytes(): Long = try { android.os.StatFs(appContext.filesDir.path).availableBytes } catch (_: Exception) { Long.MAX_VALUE }
    private fun stalledMessage(default: String) =
        if (storageIsLow(freeBytes())) "The phone is almost out of free storage, so the download stopped. Free some space, then try again." else default
    private val manager: OfflineManager
    private val regions = mutableMapOf<Long, OfflineRegion>()
    private val mutableAreas = MutableStateFlow<List<OfflineArea>>(emptyList())
    val areas = mutableAreas.asStateFlow()
    var error = MutableStateFlow<String?>(null); private set
    private var creating = false
    private var loaded = false
    private val mutableCurrent = MutableStateFlow<Long?>(null)
    /** Id of the pack created by the latest [startDownload], once native code has created it. */
    val currentId = mutableCurrent.asStateFlow()
    private var downloadSession = 0
    private val predicted = mutableMapOf<Long, DownloadPlan.Ready>()
    init {
        MapLibre.getInstance(context.applicationContext); MapNetwork.install()
        manager = OfflineManager.getInstance(context.applicationContext)
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) { offlineRegions.orEmpty().forEach { attach(it) }; loaded = true }
            override fun onError(message: String) { error.value = "Could not read downloaded maps. Try reopening Settings." }
        })
    }
    private fun update(id: Long, change: (OfflineArea) -> OfflineArea) {
        mutableAreas.value = mutableAreas.value.map { if (it.id == id) change(it) else it }
    }
    private fun attach(region: OfflineRegion) {
        regions[region.id] = region
        val definition = region.definition as? OfflineTilePyramidRegionDefinition
        val box = definition?.bounds?.let { GeoBounds(it.latitudeSouth, it.longitudeWest, it.latitudeNorth, it.longitudeEast) }
        mutableAreas.value = mutableAreas.value.filterNot { it.id == region.id } + OfflineArea(region.id, region.metadata.toString(Charsets.UTF_8),
            bounds = box, dark = definition?.styleURL?.contains("dark"),
            minZoom = definition?.minZoom?.toInt() ?: OfflineLimits.MIN_ZOOM, maxZoom = definition?.maxZoom?.toInt() ?: OfflineLimits.MAX_ZOOM)
        fun status(status: OfflineRegionStatus) {
            update(region.id) { it.copy(bytes = status.completedResourceSize, complete = status.isComplete,
                tiles = status.completedTileCount, tileBytes = status.completedTileSize,
                progress = if (status.requiredResourceCount > 0) (status.completedResourceCount.toFloat() / status.requiredResourceCount).coerceIn(0f, 1f) else 0f) }
            if (status.isComplete) predicted[region.id]?.let { p ->
                android.util.Log.i("AriciuMap", "offline pack ${region.id} complete: tiles ${status.completedTileCount} (planned ${p.tiles}), tile bytes ${status.completedTileSize} (${if (status.completedTileCount > 0) status.completedTileSize / status.completedTileCount else 0} per tile), total ${status.completedResourceSize} bytes (estimated ${p.estimatedBytes})")
                predicted.remove(region.id)
            }
            if (status.isComplete || status.completedResourceSize >= OfflineLimits.SOFT_BYTES || status.completedTileCount >= OfflineLimits.SOFT_TILES) {
                pause(region.id)
                if (!status.isComplete) update(region.id) { it.copy(limitReached = true, error = "This area is bigger than the download limit allows, so it stopped before it was complete.") }
            }
        }
        region.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(value: OfflineRegionStatus) = status(value)
            override fun onError(value: OfflineRegionError) {
                pause(region.id)
                update(region.id) { it.copy(error = stalledMessage("Download paused. Check internet and storage, then Resume. Very busy areas may reach the download limit."))}
            }
            override fun mapboxTileCountLimitExceeded(limit: Long) { pause(region.id); update(region.id) { it.copy(error = "Download limit reached.") } }
        })
        region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
            override fun onStatus(value: OfflineRegionStatus?) { value?.let { status(it) } }
            override fun onError(message: String?) { update(region.id) { it.copy(error = "Could not read map storage.") } }
        })
    }
    /**
     * Downloads a planned area for one theme. The number of saved areas is not limited. The caller shows [error] and
     * follows progress via [areas] and [currentId].
     */
    fun startDownload(plan: DownloadPlan.Ready, dark: Boolean) {
        if (!loaded) { error.value = "Offline map storage is still opening. Please try again."; return }
        if (creating || mutableAreas.value.any { it.active }) { error.value = "Finish or pause the current download first."; return }
        val needed = storageNeededFor(plan)
        if (!hasRoomFor(freeBytes(), needed)) {
            error.value = "Not enough free storage: this area needs up to ${needed / 1_000_000} MB plus some room for the phone. Free some space or delete an offline area."; return
        }
        creating = true
        mutableCurrent.value = null
        fun create() {
            val b = plan.bounds
            val definition = OfflineTilePyramidRegionDefinition(if (dark) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/positron",
                LatLngBounds.from(b.north, b.east, b.south, b.west), plan.minZoom.toDouble(), plan.maxZoom.toDouble(), 1f)
            val name = "%.3f, %.3f · %s".format(java.util.Locale.ROOT, (b.north + b.south) / 2, (b.east + b.west) / 2, if (dark) "Dark" else "Light")
            manager.createOfflineRegion(definition, name.toByteArray(), object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(region: OfflineRegion) { creating = false; predicted[region.id] = plan; attach(region); mutableCurrent.value = region.id; resume(region.id) }
                override fun onError(message: String) { creating = false; error.value = "Could not create offline area. Check free storage and try again." }
            })
        }
        create()
    }
    /** Stops a download and removes the partial pack so it is not left behind. */
    fun cancel(id: Long) = delete(id)
    fun resume(id: Long) {
        if (mutableAreas.value.any { it.active }) return
        val region = regions[id] ?: return
        if ((mutableAreas.value.firstOrNull { it.id == id }?.bytes ?: 0) >= OfflineLimits.SOFT_BYTES) { error.value = "This pack is too large. Delete it and choose another area."; return }
        MapNetwork.begin(mutableAreas.value.firstOrNull { it.id == id }?.bytes ?: 0); update(id) { it.copy(active = true, error = null, limitReached = false) }
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        val session = ++downloadSession
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (session == downloadSession && mutableAreas.value.any { it.id == id && it.active }) {
                pause(id); update(id) { it.copy(error = stalledMessage("Download paused after 90 seconds. Check internet, then continue the download.")) }
            }
        }, 90_000)
    }
    fun pause(id: Long) {
        val wasActive = mutableAreas.value.any { it.id == id && it.active }
        regions[id]?.setDownloadState(OfflineRegion.STATE_INACTIVE); update(id) { it.copy(active = false) }
        if (wasActive) { downloadSession++; MapNetwork.end() }
    }
    fun pauseAll() { regions.keys.toList().forEach(::pause) }
    fun delete(id: Long, onDeleted: (() -> Unit)? = null) {
        pause(id)
        val region = regions[id]
        if (region == null) { onDeleted?.invoke(); return }
        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() { regions.remove(id); mutableAreas.value = mutableAreas.value.filterNot { it.id == id }; onDeleted?.invoke() }
            override fun onError(message: String) {
                creating = false
                update(id) { it.copy(error = "Could not delete this area. Please retry.") }
                if (onDeleted != null) error.value = "Could not replace that area. Please retry."
            }
        })
    }
    companion object {
        @Volatile private var instance: OfflineMaps? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: OfflineMaps(context).also { instance = it } }
        fun styleUri(dark: Boolean) = if (dark) "asset://map/ariciu-graphite.json" else "asset://map/ariciu-light.json"
    }
}

@androidx.compose.runtime.Composable
fun PauseOfflineMapsWhenBackgrounded(offline: OfflineMaps) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner, offline) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) offline.pauseAll()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); offline.pauseAll() }
    }
}
