package com.busly.trackingbus

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class UppercaseStopNamesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        formatStopNamesProperly()
    }

    // ✅ Correct formatter
    private fun formatName(raw: String): String {
        return raw
            .replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch -> ch.uppercase() }
            }
    }

    // ✅ ONLY fix stops master node
    private fun formatStopNamesProperly() {

        val stopsRef = FirebaseDatabase.getInstance().getReference("stops")

        stopsRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                for (stopSnap in snapshot.children) {

                    val stopId = stopSnap.key ?: continue
                    val formattedName = formatName(stopId)

                    stopsRef.child(stopId)
                        .child("name")
                        .setValue(formattedName)

                    Log.d("FORMAT", "$stopId → $formattedName")
                }

                Log.d("FORMAT", "✔ Stop names formatted correctly")
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
