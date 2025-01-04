
# GPS Tracker App

A simple Android application to log GPS coordinates at regular intervals. The app saves the recorded locations in CSV format for easy analysis. This is ideal for tracking movement, creating geolocation datasets, or personal GPS-based experiments.

---

## Features

- **GPS Location Tracking**: Logs latitude and longitude coordinates with timestamps.
- **CSV Export**: Saves GPS data in a structured `.csv` file.
- **Background Functionality**: Continues tracking location even when the app is in the background.
- **User-Friendly Interface**: Start and stop GPS logging with a single button tap in a minimalist GUI.

---

## File Structure

The app saves data in the following directory:

```
/storage/emulated/0/Android/data/com.example.gpstracker/files/GPSLogs/
```

Each log file is named with a timestamp, e.g., `gps_log_2025-01-04_10-30-00.csv`.

---

## Prerequisites

1. **Android Device**: Minimum SDK version 23 (Android 6.0, Marshmallow).
2. **Permissions**: The app requires the following permissions:
   - `ACCESS_FINE_LOCATION`
   - `ACCESS_COARSE_LOCATION`
   - (Optional) `FOREGROUND_SERVICE` for background operation.

---

## How to Use

1. **Install the APK**:
   - Download the APK file and install it on your Android device.
   - Ensure location permissions are granted during the installation process.

2. **Start Logging**:
   - Open the app and click the **Start** button to begin logging GPS coordinates.
   - The app will create a CSV file in the `GPSLogs` directory.

3. **Stop Logging**:
   - Click the **Stop** button to stop logging. The current session's data will be finalized.

4. **Access the Logs**:
   - Use a file manager to navigate to the directory:
     ```
     /storage/emulated/0/Android/data/com.example.gpstracker/files/GPSLogs/
     ```
   - Transfer the CSV files to your computer for analysis.

---

## Known Issues

1. **Irregular Logging**:
   - GPS data may not log consistently if the device loses GPS signal.
   - Ensure the device has a clear view of the sky.

2. **Battery Optimization**:
   - Some devices may stop the app from running in the background due to aggressive battery optimization. Manually exempt the app from battery optimization.