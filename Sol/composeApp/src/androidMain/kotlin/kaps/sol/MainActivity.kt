package kaps.sol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.preference.PreferenceManager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

import android.Manifest
//import android.os.Bundle
//import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {

    private val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions if not already granted
        if (!hasLocationPermissions()) {
            ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, 0)
        }

        setContent {
            MapScreen()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return LOCATION_PERMISSIONS.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // You can check grantResults here if you want to enable location features only when granted
    }
}


@Composable
fun MapScreen() {
    var controller by remember { mutableStateOf<MapController?>(null) }

    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller = MapControllerImpl(this)

                // initial view
                controller?.setCenter(LatLng(55.6761, 12.5683), zoom = 12.0)
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // Example: add a marker from Compose after the map is created
    LaunchedEffect(controller) {
        controller?.addMarker(LatLng(55.6761, 12.5683), "Hello OSM!")
    }
}

class MapControllerImpl(private val mapView: MapView) : MapController {
    override fun setCenter(latLng: LatLng, zoom: Double) {
        val gp = GeoPoint(latLng.lat, latLng.lon)
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(gp)
    }

    override fun addMarker(latLng: LatLng, title: String?) {
        val marker = Marker(mapView).apply {
            position = GeoPoint(latLng.lat, latLng.lon)
            this.title = title
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}