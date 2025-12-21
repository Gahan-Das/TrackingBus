package com.busly.trackingbus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.busly.trackingbus.databinding.ActivityPassengerSelectBusBinding
import com.google.firebase.database.*

data class Stop(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double
)

class PassengerSelectBusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassengerSelectBusBinding

    // In-memory cache
    private val stopNames = mutableListOf<String>()
    private val nameToIdMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPassengerSelectBusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadStopsForAutoComplete()
        setupTrackBus()
        setupSearchBuses()
    }

    // ---------------- LOAD STOPS & AUTOCOMPLETE ----------------

    private fun loadStopsForAutoComplete() {

        val stopsRef = FirebaseDatabase.getInstance().getReference("stops")

        stopsRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                stopNames.clear()
                nameToIdMap.clear()

                for (stopSnap in snapshot.children) {
                    val stopId = stopSnap.key ?: continue
                    val stopName = stopSnap.child("name").getValue(String::class.java) ?: continue

                    stopNames.add(stopName)
                    nameToIdMap[stopName] = stopId
                }

                setupAutoCompleteAdapters()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@PassengerSelectBusActivity,
                    "Failed to load stops",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupAutoCompleteAdapters() {

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            stopNames
        )

        binding.etSource.setAdapter(adapter)
        binding.etDestination.setAdapter(adapter)
    }

    // ---------------- TRACK BY BUS NUMBER ----------------

    private fun setupTrackBus() {

        binding.btnTrackBus.setOnClickListener {

            val busId = binding.etBusIdPassenger
                .text
                .toString()
                .trim()
                .uppercase()

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

            val sourceName = binding.etSource.text.toString().trim()
            val destinationName = binding.etDestination.text.toString().trim()

            val sourceId = nameToIdMap[sourceName]
            val destinationId = nameToIdMap[destinationName]

            if (sourceId == null || destinationId == null) {
                Toast.makeText(
                    this,
                    "Select source and destination from suggestions",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            searchBuses(sourceId, destinationId)
        }
    }

    private fun searchBuses(sourceId: String, destinationId: String) {

        val routesRef = FirebaseDatabase.getInstance().getReference("busRoutes")
        val matchedBuses = mutableListOf<String>()

        routesRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                for (routeSnap in snapshot.children) {

                    val routeStops = routeSnap.child("stops")
                        .children
                        .mapNotNull { it.getValue(String::class.java) }

                    if (routeStops.isEmpty()) continue

                    val busNumber = routeSnap.child("busNumber")
                        .getValue(String::class.java)
                        ?: routeSnap.key
                        ?: continue

                    val forward = routeStops
                    val backward = routeStops.reversed()

                    if (
                        canServe(forward, sourceId, destinationId) ||
                        canServe(backward, sourceId, destinationId)
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

    private fun canServe(
        route: List<String>,
        sourceId: String,
        destinationId: String
    ): Boolean {

        val sourceIndex = route.indexOf(sourceId)
        val destinationIndex = route.indexOf(destinationId)

        return sourceIndex != -1 &&
                destinationIndex != -1 &&
                sourceIndex < destinationIndex
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
