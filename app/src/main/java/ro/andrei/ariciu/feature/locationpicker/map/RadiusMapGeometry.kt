package ro.andrei.ariciu.feature.locationpicker.map

import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates

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
