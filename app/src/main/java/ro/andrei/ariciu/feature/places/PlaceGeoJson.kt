package ro.andrei.ariciu.feature.places

private fun String.jsonEscaped() = buildString {
    for (c in this@jsonEscaped) when {
        c == '"' -> append("\\\""); c == '\\' -> append("\\\\"); c == '\n' -> append("\\n"); c == '\r' -> append("\\r")
        c == '\t' -> append("\\t"); c < ' ' -> append("\\u%04x".format(c.code)); else -> append(c)
    }
}

/** Point FeatureCollection for a MapLibre GeoJSON source. Properties: id, name, cat (PlaceCategory key). */
fun placesGeoJson(places: List<CatalogPlace>): String = buildString(places.size * 110 + 40) {
    append("""{"type":"FeatureCollection","features":[""")
    places.forEachIndexed { i, p ->
        if (i > 0) append(',')
        append("""{"type":"Feature","geometry":{"type":"Point","coordinates":[""").append(p.longitude).append(',').append(p.latitude)
        append("""]},"properties":{"id":"""").append(p.id.jsonEscaped()).append("""","name":"""").append(p.name.jsonEscaped())
        append("""","cat":"""").append(PlaceCategory.of(p).key).append(""""}}""")
    }
    append("]}")
}
