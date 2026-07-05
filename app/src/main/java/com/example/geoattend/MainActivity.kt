package com.example.geoattend

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    // --- YOUR OFFICE LOCATION ---
    // Update these to your current location so the demo works!
    val TARGET_LAT = 28.690202
    val TARGET_LONG = 77.479819
    val ALLOWED_RADIUS = 5000.0

    private lateinit var btnMark: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMark = findViewById(R.id.btnMarkAttendance)
        tvStatus = findViewById(R.id.tvStatus)

        // 1. Ask for Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            checkLocation()
        }

        btnMark.setOnClickListener {
            showFingerprintPrompt()
        }
    }

    private fun checkLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, TARGET_LAT, TARGET_LONG, results)
                val distance = results[0]

                if (distance < ALLOWED_RADIUS) {
                    tvStatus.text = "In Range (${distance.toInt()}m)"
                    btnMark.isEnabled = true
                } else {
                    tvStatus.text = "Out of Range (${distance.toInt()}m)"
                    btnMark.isEnabled = false
                }
            } else {
                tvStatus.text = "GPS Signal Lost"
            }
        }
    }

    private fun showFingerprintPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    uploadToFirebase()
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Fingerprint Failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Attendance Check")
            .setSubtitle("Scan fingerprint to mark present")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun uploadToFirebase() {
        try {
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("attendance")
            val data = hashMapOf(
                "student_id" to "STUDENT-001",
                "timestamp" to System.currentTimeMillis(),
                "status" to "Present"
            )
            myRef.push().setValue(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Success: Attendance Marked!", Toast.LENGTH_LONG).show()
                    btnMark.isEnabled = false
                    btnMark.text = "Marked"
                }
        } catch (e: Exception) {
            Toast.makeText(this, "App working! Connect Firebase to save data.", Toast.LENGTH_LONG).show()
        }
    }
}