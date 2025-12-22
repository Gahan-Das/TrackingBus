package com.busly.trackingbus

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.busly.trackingbus.databinding.ActivityAuthChoiceBinding
import com.google.firebase.auth.FirebaseAuth

class AuthChoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthChoiceBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthChoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

<<<<<<< HEAD
=======

>>>>>>> 149ecc7949bb67fa63c9e8d12f0e3f18f0a8eeb1
        auth = FirebaseAuth.getInstance()

        // If already logged in, skip to role selection
        val user = auth.currentUser
        if (user != null) {
            goToRoleSelection()
        }

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) return@setOnClickListener

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    goToRoleSelection()
                }
                .addOnFailureListener {
                    println("AUTH: sign up failed: ${it.message}")
                }
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) return@setOnClickListener

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    goToRoleSelection()
                }
                .addOnFailureListener {
                    println("AUTH: sign in failed: ${it.message}")
                }
        }
    }

    private fun goToRoleSelection() {
        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish()
    }
}
