package com.example.atj.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.Locale

// Utility semplice per ottenere un luogo leggibile dal device.
// Salviamo un testo utile, non latitudine/longitudine.
object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    fun getCurrentLocationText(context: Context): String {
        if (!hasLocationPermission(context)) {
            return "Permission not granted"
        }

        return try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val providers = locationManager.getProviders(true)

            var bestLocation: Location? = null

            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                        bestLocation = location
                    }
                }
            }

            if (bestLocation == null) {
                "Unknown"
            } else {
                reverseGeocode(context, bestLocation.latitude, bestLocation.longitude)
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: ""

                val country = address.countryName ?: ""

                when {
                    city.isNotBlank() && country.isNotBlank() -> "$city, $country"
                    country.isNotBlank() -> country
                    else -> "Unknown"
                }
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}