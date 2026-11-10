package ro.andrei.ariciu.feature.places

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bundled ODbL derivative; queries never leave the device. */
object LocalPlaces {
    @Volatile private var cached: List<CatalogPlace>? = null
    suspend fun load(context: Context): List<CatalogPlace> = cached ?: withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        cached ?: context.assets.open("places/romania.tsv").bufferedReader().useLines { lines ->
            lines.filter { !it.startsWith("#") && it.isNotBlank() }.map { line ->
                val p = line.split('\t')
                CatalogPlace(p[0], p[1], p[2], p[3], p[4].toDouble(), p[5].toDouble(), p.getOrElse(6) { "" })
            }.toList()
        }.also { cached = it; android.util.Log.i("AriciuMap", "catalogue: ${it.size} rows loaded in ${(System.nanoTime() - start) / 1_000_000} ms") }
    }
}
