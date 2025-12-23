package com.busly.trackingbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*

class PassengerMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference
    private var busNumber: String = ""
    private val busMarkers = HashMap<String, Marker>()
    private var hasAdjustedCamera = false

    private var lastZoomBucket = -1
    private var currentBusIcon: BitmapDescriptor? = null

    private companion object {
        const val MAX_AGE_MS = 15_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passenger_map)

        busNumber = intent.getStringExtra("busNumber")?.trim() ?: ""

        if (busNumber.isBlank()) {
            finish()
            return
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        database = FirebaseDatabase.getInstance(
            "https://tracking-bus-1c505-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("buses")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupZoomListener()
        startListeningForBusUpdates()
    }

    // ---------------- ZOOM ----------------

    private fun setupZoomListener() {
        mMap.setOnCameraIdleListener {
            val bucket = getZoomBucket(mMap.cameraPosition.zoom)
            if (bucket != lastZoomBucket) {
                currentBusIcon = getBusIconForBucket(bucket)
                busMarkers.values.forEach { it.setIcon(currentBusIcon) }
                lastZoomBucket = bucket
            }
        }
    }

    private fun getZoomBucket(zoom: Float): Int = when {
        zoom >= 19f -> 11
        zoom >= 18f -> 10
        zoom >= 17f -> 9
        zoom >= 16f -> 8
        zoom >= 15f -> 7
        zoom >= 14f -> 6
        zoom >= 13f -> 5
        zoom >= 12f -> 4
        zoom >= 11f -> 3
        zoom >= 10f -> 2
        else -> 1
    }

    private fun getBusIconForBucket(bucket: Int): BitmapDescriptor {
        val resId = when (bucket) {
            11 -> R.drawable.bus_flat_96
            10 -> R.drawable.bus_flat_80
            9  -> R.drawable.bus_flat_64
            8  -> R.drawable.bus_flat_56
            7  -> R.drawable.bus_flat_48
            6  -> R.drawable.bus_flat_40
            5  -> R.drawable.bus_flat_32
            4  -> R.drawable.bus_flat_28
            3  -> R.drawable.bus_flat_24
            2  -> R.drawable.bus_flat_20
            else -> R.drawable.bus_flat_16
        }
        return BitmapDescriptorFactory.fromResource(resId)
    }

    // ---------------- FIREBASE ----------------

    private fun startListeningForBusUpdates() {
        if (busNumber.isEmpty()) return

        database.child(busNumber).addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val now = System.currentTimeMillis()
                val activeDrivers = HashSet<String>()

                for (driverSnapshot in snapshot.children) {

                    val driverId = driverSnapshot.key ?: continue
                    val lat = driverSnapshot.child("latitude").getValue(Double::class.java)
                    val lon = driverSnapshot.child("longitude").getValue(Double::class.java)
                    val lastUpdated = driverSnapshot.child("lastUpdated").getValue(Long::class.java)
                    val online = driverSnapshot.child("online").getValue(Boolean::class.java) ?: false
                    val destination = driverSnapshot.child("destination").getValue(String::class.java) ?: "Destination not set"

                    if (lat == null || lon == null || lastUpdated == null) continue
                    if (!online || now - lastUpdated > MAX_AGE_MS) continue

                    activeDrivers.add(driverId)

                    val position = LatLng(lat, lon)

                    val marker = busMarkers[driverId]

                    if (marker == null) {
                        val newMarker = mMap.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title("Bus $busNumber")
                                .snippet("Towards: $destination")
                                .icon(
                                    currentBusIcon
                                        ?: getBusIconForBucket(
                                            getZoomBucket(mMap.cameraPosition.zoom)
                                        )
                                )
                                .anchor(0.5f, 0.5f)         
                        )


                        if (newMarker != null) {
                            busMarkers[driverId] = newMarker
                        }

                    } else {
                        marker.position = position
                    }
                }

                // 🔴 REMOVE OFFLINE / DEAD MARKERS
                busMarkers.keys
                    .filter { it !in activeDrivers }
                    .forEach {
                        busMarkers[it]?.remove()
                        busMarkers.remove(it)
                    }

                // 🔴 CAMERA LOGIC (THE IMPORTANT PART)
                if (!hasAdjustedCamera && busMarkers.isNotEmpty()) {

                    if (busMarkers.size == 1) {
                        // Single bus → zoom in
                        val pos = busMarkers.values.first().position
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(pos, 16f)
                        )
                    } else {
                        // Multiple buses → fit all
                        val boundsBuilder = LatLngBounds.Builder()
                        busMarkers.values.forEach {
                            boundsBuilder.include(it.position)
                        }

                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                boundsBuilder.build(),
                                120
                            )
                        )
                    }

                    hasAdjustedCamera = true
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

}
