package com.busly.trackingbus

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var busId: String? = null
    private val driverId by lazy {
        FirebaseAuth.getInstance().uid ?: "driver_${System.currentTimeMillis()}"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        busId = intent?.getStringExtra("busId")

        if (busId.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "Location permission missing")
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateDistanceMeters(3f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                sendLocationToFirebase(loc)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun sendLocationToFirebase(location: android.location.Location) {
        val bus = busId ?: return

        val ref = FirebaseDatabase.getInstance(
            "https://tracking-bus-1c505-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
            .getReference("buses")
            .child(bus)
            .child(driverId)

        val update = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "bearing" to location.bearing,
            "speed" to location.speed,
            "lastUpdated" to ServerValue.TIMESTAMP,
            "online" to true
        )

        ref.updateChildren(update)
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------- NOTIFICATION ----------------

    private fun createNotification(): Notification {
        val channelId = "location_tracking"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bus Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bus tracking active")
            .setContentText("Sharing live location")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 101
    }
}
