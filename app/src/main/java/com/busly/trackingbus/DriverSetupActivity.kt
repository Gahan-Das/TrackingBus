package com.busly.trackingbus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.busly.trackingbus.databinding.ActivityDriverSetupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DriverSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverSetupBinding
    private lateinit var driverRef: DatabaseReference

    private var currentBusId: String? = null
    private var destination: String? = null

    private val driverId by lazy {
        FirebaseAuth.getInstance().uid ?: "driver_${System.currentTimeMillis()}"
    }

    // ---------- Permission launcher ----------
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                startTrackingAndOpenMap()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required to start tracking",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartSharing.setOnClickListener {
            val busIdInput = binding.etBusId.text.toString().trim().uppercase()
            val destInput = binding.etDestination.text.toString().trim()

            if (busIdInput.isEmpty() || destInput.isEmpty()) {
                Toast.makeText(this, "Enter bus number and destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentBusId = busIdInput
            destination = destInput

            setupPresence(busIdInput)
            driverRef.child("destination").setValue(destInput)

            checkLocationPermission()
        }
    }

    // ---------- Firebase presence ----------
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

    // ---------- Permission ----------
    private fun checkLocationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startTrackingAndOpenMap()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ---------- Start service + open map ----------
    private fun startTrackingAndOpenMap() {

        // 1️⃣ Start foreground service
        val serviceIntent = Intent(this, LocationService::class.java).apply {
            putExtra("busId", currentBusId)
            putExtra("driverId", driverId)
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        // 2️⃣ Open map activity
        val mapIntent = Intent(this, DriverMapActivity::class.java).apply {
            putExtra("busId", currentBusId)
            putExtra("destination", destination)
        }
        startActivity(mapIntent)

        // 3️⃣ Kill setup screen
        finish()
    }
}
