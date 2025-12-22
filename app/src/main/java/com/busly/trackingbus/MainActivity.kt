package com.busly.trackingbus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.busly.trackingbus.ui.theme.TrackingBusTheme
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            println("PERMISSION CALLBACK: $result")

            val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            println("PERMISSION CALLBACK PARSED: fine=$fine coarse=$coarse")

            if (fine || coarse) {
                println("PERMISSION CALLBACK: Permission granted → starting updates")
                startLocationUpdates()
            } else {
                println("PERMISSION CALLBACK: Permission denied")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("MAIN: onCreate called")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            TrackingBusTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Greeting("Android", Modifier.padding(padding))
                }
            }
        }

        println("MAIN: calling requestLocationPermission()")
        requestLocationPermission()
    }


    private fun requestLocationPermission() {
        println("MAIN: in requestLocationPermission()")

        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        println("MAIN: fineGranted=$fineGranted coarseGranted=$coarseGranted")

        if (fineGranted || coarseGranted) {
            println("MAIN: Permission already granted → starting updates")
            startLocationUpdates()
        } else {
            println("MAIN: Requesting location permission now…")
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationUpdates() {
        println("LOCATION: startLocationUpdates() called")

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        println("LOCATION: onLocationResult() called")

                        val loc = result.lastLocation
                        if (loc == null) {
                            println("LOCATION: lastLocation == null")
                            return
                        }

                        println("LOCATION: ${loc.latitude}, ${loc.longitude}")
                        sendLocationToFirebase(loc.latitude, loc.longitude)
                    }
                },
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            println("LOCATION: SecurityException: ${e.message}")
            e.printStackTrace()
        }
    }


    private fun sendLocationToFirebase(lat: Double, lon: Double) {
        println("FIREBASE: sending location lat=$lat lon=$lon")

        val busId = "BUS_101"

        val data = mapOf(
            "lat" to lat,
            "lon" to lon,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance("https://tracking-bus-1c505-default-rtdb.asia-southeast1.firebasedatabase.app")

            .getReference("buses")
            .child(busId)
            .setValue(data)
            .addOnSuccessListener {
                println("FIREBASE: write success")
            }
            .addOnFailureListener {
                println("FIREBASE: write FAILED: ${it.message}")
            }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TrackingBusTheme {
        Greeting("Android")
    }
}
