package com.example.gpstracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    private lateinit var locationManager: LocationManager
    private val locationListener = LocationListener { location ->
        logLocation(location)
    }
    private var logFile: File? = null

    override fun onCreate() {
        super.onCreate()

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Create the folder and file
        val folder = File(getExternalFilesDir(null), "GPSLogs")
        if (!folder.exists()) folder.mkdirs()
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        logFile = File(folder, "gps_log_$timestamp.csv")
        if (logFile?.createNewFile() == true) {
            FileWriter(logFile, true).use {
                it.append("datetime,latitude,longitude\n")
            }
        }

        // Start foreground service
        startForegroundService()
        requestLocationUpdates()
    }

    private fun startForegroundService() {
        val channelId = "LocationServiceChannel"
        val channelName = "GPS Tracker Background Service"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (notificationManager?.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GPS Tracker")
            .setContentText("Tracking location in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            1f,
            locationListener,
            Looper.getMainLooper()
        )
    }

    private fun logLocation(location: Location) {
        val datetime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val latitude = location.latitude
        val longitude = location.longitude

        try {
            FileWriter(logFile, true).use {
                it.append("$datetime,$latitude,$longitude\n")
            }
        } catch (e: IOException) {
            Log.e("LocationService", "Failed to log location: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }
}
