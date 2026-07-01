package com.example.atj.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.Locale

/*
 * Utility per ricavare una posizione testuale dal dispositivo.
 * Usa i servizi di localizzazione Android e il reverse geocoding.
 */
object LocationHelper {

    /*
     * Controlla se l'app ha almeno un permesso di localizzazione.
     * Android richiede sia dichiarazione nel Manifest sia controllo runtime.
     */
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

    /*
     * Recupera l'ultima posizione nota e la converte in testo.
     * LocationManager è un System Service ottenuto tramite Context.
     */
    fun getCurrentLocationText(context: Context): String {
        if (!hasLocationPermission(context)) {
            return "Permission not granted"
        }

        return try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val providers = locationManager.getProviders(true)

            var bestLocation: Location? = null

            /*
             * Tra i provider disponibili sceglie la posizione con accuratezza migliore.
             */
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

    /*
     * Reverse geocoding: converte latitudine e longitudine in città/nazione.
     */
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