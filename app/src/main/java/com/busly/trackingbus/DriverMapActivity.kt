package com.busly.trackingbus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var driverRef: DatabaseReference

    private var driverMarker: Marker? = null
    private var hasAdjustedCamera = false
    private var followBus = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_map)

        val busId = intent.getStringExtra("busId")
        val driverId = FirebaseAuth.getInstance().uid

        if (busId == null || driverId == null) {
            Toast.makeText(this, "Invalid driver session", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        driverRef = FirebaseDatabase.getInstance(
            "https://tracking-bus-1c505-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
            .getReference("buses")
            .child(busId)
            .child(driverId)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<View>(R.id.btnStopSharing).setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
            finish()
        }
    }

    // ---------------- MAP ----------------

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        }

        mMap.uiSettings.isMyLocationButtonEnabled = true

        // If driver manually moves map → pause auto-follow
        mMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                followBus = false
            }
        }

        // My Location button or programmatic move → resume follow
        mMap.setOnCameraIdleListener {
            followBus = true
        }

        startListeningForDriverLocation()
    }

    // ---------------- FIREBASE ----------------

    private fun startListeningForDriverLocation() {

        driverRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)
                val bearing =
                    snapshot.child("bearing").getValue(Float::class.java) ?: 0f

                if (lat == null || lng == null) return

                val position = LatLng(lat, lng)

                // -------- MARKER UPDATE --------

                if (driverMarker == null) {
                    driverMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .anchor(0.5f, 0.5f)
                            .icon(BitmapDescriptorFactory.defaultMarker())
                    )
                } else {
                    driverMarker!!.position = position
                }

                // -------- CAMERA LOGIC (KEY FIX) --------

                if (!hasAdjustedCamera) {
                    // 🔥 FIRST ZOOM (PassengerMap pattern)
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(position, 16f)
                    )
                    hasAdjustedCamera = true
                    followBus = true
                    return
                }

                // -------- AUTO-FOLLOW --------

                if (followBus) {
                    val cameraPosition = CameraPosition.Builder()
                        .target(position)
                        .zoom(mMap.cameraPosition.zoom)
                        .bearing(bearing)
                        .tilt(45f)
                        .build()

                    mMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(cameraPosition),
                        800,
                        null
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
