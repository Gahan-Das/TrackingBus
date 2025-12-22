package com.busly.trackingbus

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.busly.trackingbus.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDriver.setOnClickListener {
            startActivity(Intent(this, DriverSetupActivity::class.java))
        }

        binding.btnPassenger.setOnClickListener {
            startActivity(Intent(this, PassengerSelectBusActivity::class.java))
        }
    }
}
