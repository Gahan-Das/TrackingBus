package com.busly.trackingbus

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.busly.trackingbus.databinding.ActivityUpdateRouteBinding
import com.google.firebase.database.FirebaseDatabase

class UpdateRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateRouteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmitRoute.setOnClickListener {
            val busNumber = binding.etBusNumber.text.toString().trim().uppercase()
            val routeString = binding.etRouteString.text.toString().trim()

            if (busNumber.isEmpty() || routeString.isEmpty()) {
                Toast.makeText(this, "Enter bus number and route", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Split route by "->"
            val stopsList = routeString.split("->").map { it.trim() }

            saveRouteToFirebase(busNumber, stopsList)
        }
    }

    private fun saveRouteToFirebase(busNumber: String, stops: List<String>) {
        val routeRef = FirebaseDatabase.getInstance(
            "https://tracking-bus-1c505-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
            .getReference("busRoutes")
            .child(busNumber)

        val routeData = mutableMapOf<String, Any>()
        routeData["busNumber"] = busNumber

        val stopsMap = mutableMapOf<String, Any>()
        stops.forEachIndexed { index, stop ->
            stopsMap[index.toString()] = stop
        }
        routeData["stops"] = stopsMap

        routeRef.setValue(routeData)
            .addOnSuccessListener {
                Toast.makeText(this, "Route uploaded successfully", Toast.LENGTH_SHORT).show()
                finish() // close activity after success
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to upload route", Toast.LENGTH_SHORT).show()
            }
    }
}
