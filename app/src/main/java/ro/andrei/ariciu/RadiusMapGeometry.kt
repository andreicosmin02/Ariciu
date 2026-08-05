package ro.andrei.ariciu

import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position

fun List<SelectedCoordinates>.toGeoJsonPolygon(): Polygon = Polygon(
    coordinates = listOf(
        map { coordinate ->
            Position(
                longitude = coordinate.longitude,
                latitude = coordinate.latitude,
            )
        }
    )
)
