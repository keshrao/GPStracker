package com.example.gpstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.CountDownTimer

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var locationTextView: TextView
    private val locationPermissionCode = 2
    private var isLogging = false
    private var logFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private val logDuration = 2 * 60 * 60 * 1000L // 2 hours in milliseconds
    private lateinit var countDownTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)
        locationTextView = findViewById(R.id.locationTextView)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Check for battery optimization exemption
        // checkAndRequestBatteryOptimizationExemption()  // Removing this menu pop-up

        startButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                isLogging = true
                createNewLogFile()
                startLocationService()
                getLocation()
                startLogTimer() // Start the timer when logging starts
            }
        }

        stopButton.setOnClickListener {
            stopLogging()
            stopLocationService()
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), locationPermissionCode)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted; notify the user and allow logging to start
                Log.d("GPSLogs", "Permissions granted. Ready to start logging.")
            } else {
                // Permission was denied
                Log.e("GPSLogs", "Required permissions not granted.")
            }
        }
    }

    private fun createNewLogFile() {
        Log.d("GPSLogs", "Creating new log file")
        val folder = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "GPSLogs")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        logFile = File(folder, "gps_log_$timestamp.csv")
        Log.d("GPSLogs", "File path: ${logFile?.absolutePath}")
        try {
            if (logFile?.createNewFile() == true) {
                FileWriter(logFile, true).use {
                    it.append("datetime,latitude,longitude\n")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startLogTimer() {
        countDownTimer = object : CountDownTimer(logDuration, 1000) { // 2 hours in milliseconds
            override fun onTick(millisUntilFinished: Long) {
                // Log remaining time if needed
                Log.d("GPSLogs", "Time remaining: ${millisUntilFinished / 1000} seconds")
            }

            override fun onFinish() {
                // Stop logging when the timer finishes
                stopLogging()
                Log.d("GPSLogs", "Logging stopped automatically after 2 hours")
            }
        }
        countDownTimer.start()
    }

    private fun stopLogging() {
        isLogging = false
        locationManager.removeUpdates(locationListener)
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        locationTextView.text = ""
        Log.d("GPSLogs", "Logging stopped manually")
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLogging() // Ensure logging stops when the app is destroyed
        Log.d("GPSLogs", "App destroyed, logging stopped")
    }

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            Log.d("GPSLogs", "Requesting location updates")
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                1f,
                locationListener
            )
        }
    }

    private fun checkAndRequestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                requestBatteryOptimizationExemption()
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    private val locationListener = LocationListener { location ->

        // Skip processing if logging is stopped
        if (!isLogging) {
            return@LocationListener
        }

        val datetime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val latitude = location.latitude
        val longitude = location.longitude
        locationTextView.text = "Location: $latitude, $longitude"

        Log.d("GPSLogs", "Location update received: Lat = ${location.latitude}, Lon = ${location.longitude}")

        // Write to CSV file if logging is enabled
        if (isLogging) {
            try {
                FileWriter(logFile, true).use {
                    it.append("$datetime,$latitude,$longitude\n")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
