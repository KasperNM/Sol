package kaps.sol

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
// hej
class MainActivity : ComponentActivity() {

    private val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize osmdroid configuration
        Configuration.getInstance().userAgentValue = "SolApp/1.0 (kaps.sol)"
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        // Request location permissions if not granted
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
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

@Composable
fun MapScreen() {
    var cafes by remember { mutableStateOf<List<Pair<GeoPoint, String>>>(emptyList()) }

    // 🔹 Hent cafédata når skærmen starter
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cafes = fetchCafes()
        }
    }

    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(13.0)
                controller.setCenter(GeoPoint(55.6761, 12.5683)) // København
            }
        },
        update = { map ->
            map.overlays.clear()

            // ☕️ Brug en lille PNG direkte som ikon
            val cafeIcon = ContextCompat.getDrawable(map.context, R.drawable.coffeecup)

            cafes.take(20).forEach { (point, name) -> // viser kun 20 caféer
                val marker = Marker(map)
                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = name
                marker.icon = cafeIcon
                map.overlays.add(marker)
            }

            map.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Hent fortovscaféer fra OpenStreetMap (Overpass API)
 */
fun fetchCafes(): List<Pair<GeoPoint, String>> {
    val client = OkHttpClient()

    // Overpass forespørgsel: caféer med udendørs servering i København
    val query = """
        [out:json];
        node["amenity"="cafe"]["outdoor_seating"="yes"](55.65,12.50,55.70,12.60);
        out;
    """.trimIndent()

    val url = "https://overpass-api.de/api/interpreter?data=${query}"

    return try {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val elements = json.getJSONArray("elements")

            val cafes = mutableListOf<Pair<GeoPoint, String>>()
            for (i in 0 until minOf(elements.length(), 20)) { // max 20 caféer
                val node = elements.getJSONObject(i)
                val lat = node.getDouble("lat")
                val lon = node.getDouble("lon")
                val name = node.optJSONObject("tags")?.optString("name") ?: "Fortovscafé"
                cafes.add(Pair(GeoPoint(lat, lon), name))
            }
            cafes
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
