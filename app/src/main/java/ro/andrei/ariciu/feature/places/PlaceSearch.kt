package ro.andrei.ariciu.feature.places

const val SEARCH_RADIUS_METERS = 25_000.0
const val SEARCH_RESULT_LIMIT = 40

private fun fold(value: String) = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)
private fun words(value: String) = fold(value).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

/**
 * Ranked search over [places] (the cells around the reference point) plus optional nationwide [towns].
 * Match = every query word is a prefix of a word in name, brand, category words or address.
 * Rank: exact name, then name/brand, then category words, then address; ties by distance then id.
 * Places farther than [radiusMeters] are dropped; towns are the only exception and only on a name match.
 * An empty query needs at least one category (chip).
 */
fun searchCatalog(places: List<CatalogPlace>, query: String, categories: Set<PlaceCategory>, latitude: Double, longitude: Double,
    radiusMeters: Double = SEARCH_RADIUS_METERS, limit: Int = SEARCH_RESULT_LIMIT, towns: List<CatalogPlace> = emptyList()): List<CatalogPlace> {
    val q = words(query)
    if (q.isEmpty() && categories.isEmpty()) return emptyList()
    fun prefixAll(tokens: List<String>) = q.all { w -> tokens.any { it.startsWith(w) } }
    fun dist(p: CatalogPlace) = distanceMeters(latitude, longitude, p.latitude, p.longitude)
    val scored = ArrayList<Triple<CatalogPlace, Int, Double>>()
    val seen = HashSet<String>()
    for (p in places) {
        val category = PlaceCategory.of(p)
        if (categories.isNotEmpty() && category !in categories) continue
        val d = dist(p)
        if (d > radiusMeters) continue
        val rank = if (q.isEmpty()) 0 else {
            val name = words(p.name); val nameBrand = name + words(p.chain)
            val catWords = words(category.label) + category.aliases
            when {
                name == q -> 0
                prefixAll(nameBrand) -> 1
                prefixAll(nameBrand + catWords) -> 2
                prefixAll(nameBrand + catWords + words(p.address)) -> 3
                else -> continue
            }
        }
        if (seen.add(p.id)) scored += Triple(p, rank, d)
    }
    if (q.isNotEmpty() && (categories.isEmpty() || PlaceCategory.TOWN in categories)) for (t in towns) {
        if (t.id in seen) continue
        val name = words(t.name)
        val rank = when { name == q -> 0; prefixAll(name) -> 1; else -> continue }
        seen += t.id; scored += Triple(t, rank, dist(t))
    }
    return scored.sortedWith(compareBy<Triple<CatalogPlace, Int, Double>> { it.second }.thenBy { it.third }.thenBy { it.first.id })
        .take(limit.coerceAtLeast(0)).map { it.first }
}
