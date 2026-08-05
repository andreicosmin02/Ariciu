package ro.andrei.ariciu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private const val OPEN_FREE_MAP_POSITRON_STYLE_URL =
    "https://tiles.openfreemap.org/styles/positron"
private const val BRASOV_DEVELOPMENT_LATITUDE = 45.6579
private const val BRASOV_DEVELOPMENT_LONGITUDE = 25.6012
private const val BRASOV_DEVELOPMENT_ZOOM = 13.0

@Composable
fun LocationPickerScreen() {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(
                latitude = BRASOV_DEVELOPMENT_LATITUDE,
                longitude = BRASOV_DEVELOPMENT_LONGITUDE,
            ),
            zoom = BRASOV_DEVELOPMENT_ZOOM,
        )
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Choose location",
                style = MaterialTheme.typography.headlineSmall
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .testTag("location_picker_map_area"),
                contentAlignment = Alignment.Center
            ) {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_POSITRON_STYLE_URL),
                    cameraState = cameraState,
                )
            }
        }
    }
}
