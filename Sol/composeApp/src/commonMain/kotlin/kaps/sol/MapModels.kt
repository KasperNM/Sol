package kaps.sol

import kotlinx.serialization.Serializable

@Serializable
data class LatLng(val lat: Double, val lon: Double)

// Interface for platform-specific controllers (Android/iOS will implement this)
interface MapController {
    fun setCenter(latLng: LatLng, zoom: Double)
    fun addMarker(latLng: LatLng, title: String?)
}

// Helper for OSM tiles
fun tileUrlTemplateForOSM(): String = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
