package ro.andrei.ariciu.feature.offlinemap

import java.io.IOException

/** Count every response byte, including retries, before handing resources to native code. */
class DownloadBudget(initialBytes: Long = 0) {
    var bytes: Long = initialBytes; private set
    var tiles: Int = 0; private set
    @Synchronized fun consume(count: Int, tile: Boolean = false) {
        require(count >= 0)
        bytes += count
        if (tile) tiles++
        if (bytes > 45_000_000 || tiles > 2500) throw IOException("Offline download safety limit reached")
    }
}
