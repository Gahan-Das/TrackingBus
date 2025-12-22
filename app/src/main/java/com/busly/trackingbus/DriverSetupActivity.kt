package com.busly.trackingbus

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.busly.trackingbus.databinding.ActivityDriverSetupBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DriverSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverSetupBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var driverRef: DatabaseReference

    private var currentBusId: String? = null

    private val driverId by lazy {
        FirebaseAuth.getInstance().uid ?: "driver_${System.currentTimeMillis()}"
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) startLocationUpdates()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Start sharing location
        binding.btnStartSharing.setOnClickListener {
            val busId = binding.etBusId.text.toString().trim().uppercase()
            val destination = binding.etDestination.text.toString().trim()

            if (busId.isEmpty() || destination.isEmpty()) {
                Toast.makeText(this, "Fill bus number and destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentBusId = busId
            setupPresence(busId)

            driverRef.child("destination").setValue(destination)

            requestLocationPermission()
        }

        // NEW: Update/Insert Route button
        binding.btnUpdateRoute.setOnClickListener {
            val intent = Intent(this, UpdateRouteActivity::class.java)
            startActivity(intent)
        }

    }

    // ---------------- PRESENCE ----------------

    private fun setupPresence(busId: String) {
        driverRef = FirebaseDatabase.getInstance(
            "https://tracking-bus-1c505-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
            .getReference("buses")
            .child(busId)
            .child(driverId)

        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    driverRef.child("online").setValue(true)
                    driverRef.onDisconnect().updateChildren(
                        mapOf(
                            "online" to false,
                            "lastUpdated" to ServerValue.TIMESTAMP
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---------------- LOCATION ----------------

    private fun requestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            startLocationUpdates()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationUpdates() {
        val busId = currentBusId ?: return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateDistanceMeters(3f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    sendLocationToFirebase(busId, loc)
                }
            },
            Looper.getMainLooper()
        )
    }

    // ---------------- FIREBASE UPDATE ----------------

    private fun sendLocationToFirebase(busId: String, location: android.location.Location) {
        val update = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "lastUpdated" to ServerValue.TIMESTAMP,
            "online" to true
        )
        driverRef.updateChildren(update)
    }

    // ---------------- ROUTE UPLOAD ----------------

    
}
