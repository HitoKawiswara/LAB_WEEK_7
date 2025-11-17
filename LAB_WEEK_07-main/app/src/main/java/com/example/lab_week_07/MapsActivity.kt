package com.example.lab_week_07

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lab_week_07.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // (Langkah 13) Fused Location Provider API
    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Daftarkan launcher untuk single-permission (sesuai modul)
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    getLastLocation()
                } else {
                    // Tampilkan rationale dialog: user harus klik OK agar kita minta izin lagi
                    showPermissionRationale {
                        requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                    }
                }
            }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Begitu map siap, jalankan alur izin:
        when {
            hasLocationPermission() -> getLastLocation()
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
            else -> requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    // ====== Helper ======
    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun showPermissionRationale(onPositive: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("This app will not work without knowing your current location")
            .setPositiveButton("OK") { _, _ -> onPositive() }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    // (Langkah 14) Pindahkan kamera dan tambahkan marker
    private fun updateMapLocation(location: LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 7f))
    }

    private fun addMarkerAtLocation(location: LatLng, title: String) {
        mMap.addMarker(MarkerOptions().title(title).position(location))
    }

    // (Langkah 15) Ambil lokasi terakhir, lalu fokus kamera + marker "You"
    private fun getLastLocation() {
        if (hasLocationPermission()) {
            try {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val userLocation = LatLng(it.latitude, it.longitude)
                            updateMapLocation(userLocation)
                            addMarkerAtLocation(userLocation, "You")
                        }
                    }
            } catch (e: SecurityException) {
                Log.e("MapsActivity", "SecurityException: ${e.message}")
            }
        } else {
            // Jika izin belum diberikan, minta izin
            requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }
}
