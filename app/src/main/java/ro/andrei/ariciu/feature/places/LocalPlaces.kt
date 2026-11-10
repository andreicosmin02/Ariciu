package ro.andrei.ariciu.feature.places

import android.content.Context
import java.io.IOException

/** Bundled ODbL catalogue (towns.tsv + grid cells). Queries never leave the device. */
object LocalPlaces {
    @Volatile private var cached: PlaceIndex? = null
    fun index(context: Context): PlaceIndex = cached ?: synchronized(this) {
        cached ?: PlaceIndex { path -> try { context.applicationContext.assets.open("places/$path") } catch (_: IOException) { null } }.also { cached = it }
    }
}
