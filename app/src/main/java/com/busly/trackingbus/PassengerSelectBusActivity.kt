package com.busly.trackingbus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.busly.trackingbus.databinding.ActivityPassengerSelectBusBinding
import com.google.firebase.database.*

data class BusRoute(
    val busNumber: String? = null,
    val stops: List<String>? = null
)

class PassengerSelectBusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassengerSelectBusBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPassengerSelectBusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAutoComplete()
        setupTrackBus()
        setupSearchBuses()
    }

    // ---------------- AUTOCOMPLETE ----------------

    private fun setupAutoComplete() {

        val stopNames = mutableSetOf<String>()

        val stopRef = FirebaseDatabase.getInstance()
            .getReference("busRoutes")

        stopRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                stopNames.clear()

                for (busSnap in snapshot.children) {

                    val stopsSnap = busSnap.child("stops")
                    if (!stopsSnap.exists()) continue

                    for (stopSnap in stopsSnap.children) {
                        stopSnap.getValue(String::class.java)?.let {
                            stopNames.add(it)
                        }
                    }
                }



                val adapter = ArrayAdapter(
                    this@PassengerSelectBusActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    stopNames.toList().sorted()
                )

                binding.etSource.setAdapter(adapter)
                binding.etDestination.setAdapter(adapter)

                binding.etSource.threshold = 1
                binding.etDestination.threshold = 1

            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---------------- TRACK BY BUS NUMBER ----------------

    private fun setupTrackBus() {

        binding.btnTrackBus.setOnClickListener {

            val busId = binding.etBusIdPassenger.text.toString().trim().uppercase()

            if (busId.isEmpty()) {
                Toast.makeText(this, "Enter bus number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, PassengerMapActivity::class.java)
            intent.putExtra("busNumber", busId)
            startActivity(intent)
        }
    }

    // ---------------- SEARCH BY SOURCE & DESTINATION ----------------

    private fun setupSearchBuses() {

        binding.btnSearchBuses.setOnClickListener {

            val source = binding.etSource.text.toString().trim()
            val destination = binding.etDestination.text.toString().trim()

            if (source.isEmpty() || destination.isEmpty()) {
                Toast.makeText(this, "Enter source and destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            searchBuses(source, destination)
        }
    }

    private fun searchBuses(source: String, destination: String) {

        val ref = FirebaseDatabase.getInstance()
            .getReference("busRoutes")

        val matchedBuses = mutableListOf<String>()

        ref.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                for (busSnap in snapshot.children) {

                    // ✅ SAFELY READ STOPS
                    val stops = busSnap.child("stops").children
                        .mapNotNull { it.getValue(String::class.java) }

                    if (stops.isEmpty()) continue

                    // ✅ SAFELY READ BUS NUMBER
                    val busNumber = busSnap.child("busNumber")
                        .getValue(String::class.java)
                        ?: busSnap.key
                        ?: continue

                    val upRoute = stops
                    val downRoute = stops.reversed()

                    if (
                        canServe(upRoute, source, destination) ||
                        canServe(downRoute, source, destination)
                    ) {
                        matchedBuses.add(busNumber)
                    }
                }

                showResults(matchedBuses)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---------------- HELPERS ----------------

    private fun canServe(route: List<String>, source: String, destination: String): Boolean {
        val normalizedRoute = route.map { it.lowercase() }
        val s = normalizedRoute.indexOf(source.lowercase())
        val d = normalizedRoute.indexOf(destination.lowercase())
        return s != -1 && d != -1 && s < d
    }

    private fun showResults(buses: List<String>) {

        binding.tvSearchResults.visibility = View.VISIBLE

        binding.tvSearchResults.text =
            if (buses.isEmpty())
                "No buses found"
            else
                "Available buses: ${buses.joinToString(", ")}"
    }
}
