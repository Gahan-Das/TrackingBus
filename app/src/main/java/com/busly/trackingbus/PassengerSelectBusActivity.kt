package com.busly.trackingbus

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.busly.trackingbus.databinding.ActivityPassengerSelectBusBinding
import com.google.firebase.database.*

<<<<<<< HEAD
data class BusRoute(
    val busNumber: String? = null,
    val stops: List<String>? = null
=======
data class Stop(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double
>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1
)

class PassengerSelectBusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassengerSelectBusBinding

<<<<<<< HEAD
=======
    // In-memory cache
    private val stopNames = mutableListOf<String>()
    private val nameToIdMap = mutableMapOf<String, String>()

>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPassengerSelectBusBinding.inflate(layoutInflater)
        setContentView(binding.root)

<<<<<<< HEAD
        setupAutoComplete()
=======
        loadStopsForAutoComplete()
>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1
        setupTrackBus()
        setupSearchBuses()
    }

<<<<<<< HEAD
    // ---------------- AUTOCOMPLETE ----------------

    private fun setupAutoComplete() {

        val stopNames = mutableSetOf<String>()

        val stopRef = FirebaseDatabase.getInstance()
            .getReference("busRoutes")

        stopRef.addListenerForSingleValueEvent(object : ValueEventListener {
=======
    // ---------------- LOAD STOPS & AUTOCOMPLETE ----------------

    private fun loadStopsForAutoComplete() {

        val stopsRef = FirebaseDatabase.getInstance().getReference("stops")

        stopsRef.addListenerForSingleValueEvent(object : ValueEventListener {
>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1

            override fun onDataChange(snapshot: DataSnapshot) {

                stopNames.clear()
<<<<<<< HEAD

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

=======
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

>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1
    // ---------------- TRACK BY BUS NUMBER ----------------

    private fun setupTrackBus() {

        binding.btnTrackBus.setOnClickListener {

<<<<<<< HEAD
            val busId = binding.etBusIdPassenger.text.toString().trim().uppercase()
=======
            val busId = binding.etBusIdPassenger
                .text
                .toString()
                .trim()
                .uppercase()
>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1

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

<<<<<<< HEAD
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
=======
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
>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1
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

<<<<<<< HEAD
    private fun canServe(route: List<String>, source: String, destination: String): Boolean {
        val normalizedRoute = route.map { it.lowercase() }
        val s = normalizedRoute.indexOf(source.lowercase())
        val d = normalizedRoute.indexOf(destination.lowercase())
        return s != -1 && d != -1 && s < d
=======
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
>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1
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
