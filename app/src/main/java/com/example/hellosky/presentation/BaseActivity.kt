package com.example.hellosky.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import callAsync
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import fetchWeatherData
import glm_.quat.Quat
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.roundToInt

open class BaseActivity : ComponentActivity(), SensorEventListener {
    private var gpsPressure: Float = 0f
    private var azimuth: Float = 0f
    var azimuthRot: Float = 0f
    private val ALTITUDE_THRESHOLD = 0.25f
    private val ALTITUDE_DECIMALS = 10.0f
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val GPS_INTERVAL = 2000L

    private var pressure = 0f

    val altitudeState = mutableStateOf(0f)
    private val rawAltitudeState = mutableStateOf(0f)
    val locationState = mutableStateOf(Location(""))
    val batteryDrainPerHourState = mutableStateOf("???")
    val deviceRotationState = mutableStateOf(Quat())


    private var initialAltitude = 0f
    private var initialAltitudeSet = false
    private var lastAltitude = 0f
    private lateinit var vibrator: Vibrator
    var initialLocation: Location? = null
    private var lastLocation: Location? = null
    private var lastToastTime: Long = 0
    private var initialBattery: Int = 0
    private var initialTime: Long = 0
    private var drainedBattery: Int = 0

    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var settingsClient: SettingsClient

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null


    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            onCheckUpdates()
            handler.postDelayed(this, 5000)
        }
    }

    private fun onWeatherData(jsonResponse: JSONObject) {
        val pressure = jsonResponse.getJSONObject("main").getInt("pressure")
        this.gpsPressure = pressure.toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initSensorManager()
        initVibrator()
        initFusedLocationClient()
        initialBattery = getBatteryLevel()
        initialTime = System.currentTimeMillis()
        handler.post(runnable)
    }


    private fun onCheckUpdates() {
        // Battery
        val drainedBattery = initialBattery - getBatteryLevel()
        if (drainedBattery > this.drainedBattery) {
            this.drainedBattery = drainedBattery;

            val elapsedHours = getElapsedSecs() / 60 / 60;
            if (drainedBattery > 0) {
                batteryDrainPerHourState.value = "" + (drainedBattery / elapsedHours).roundToInt()
            }
        }
    }

    private fun initFusedLocationClient() {
//        if (ContextCompat.checkSelfPermission(
//                this, Manifest.permission.ACCESS_FINE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            toast("Init GPS")
//            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//            settingsClient = LocationServices.getSettingsClient(this)
//
//            locationRequest = LocationRequest.create().apply {
//                interval = GPS_INTERVAL
//                fastestInterval = GPS_INTERVAL
//                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//            }
//
//            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
//            settingsClient.checkLocationSettings(builder.build()).addOnSuccessListener {
//                toast("checkLocationSettings SUCCESS")
//                startLocationUpdates()
//            }.addOnFailureListener { toast("checkLocationSettings FAILURE ${it.toString()}") }
//
//            locationCallback = object : LocationCallback() {
//                override fun onLocationResult(locationResult: LocationResult) {
//                    locationResult ?: return
//                    val lastLocation = locationResult.locations.lastOrNull()
//                    lastLocation?.let {
//                        updateLocation(it)
//                    }
//                }
//            }
//        } else {
//            toast("Missing location permission")
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                LOCATION_PERMISSION_REQUEST_CODE
//            )
//            toast("Location permission done")
//            initFusedLocationClient()
//        }
    }

    fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun startLocationUpdates() {
//        if (ContextCompat.checkSelfPermission(
//                this, Manifest.permission.ACCESS_FINE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED
//        ) {
//            fusedLocationClient.requestLocationUpdates(
//                locationRequest, locationCallback, Looper.getMainLooper()
//            )
//        }
    }

    override fun onResume() {
        super.onResume()
        toast("onResume")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        toast("onPause")
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
//        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun initVibrator() {
        @Suppress("DEPRECATION") vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun initSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { rot ->
            sensorManager.registerListener(this, rot, SensorManager.SENSOR_DELAY_UI)
        }
//        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
//            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
//        }
//        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
//            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                toast("onRequestPermissionsResult")
                initFusedLocationClient()
            } else {
                toast("Permission denied")
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            pressure = event.values[0]
            val newAltitude = pressureToAltitude(pressure)

            if (!initialAltitudeSet) {
                initialAltitude = newAltitude
                initialAltitudeSet = true
            }

            // rawAltitudeState.value = newAltitude - initialAltitude

            val altDiff = newAltitude - lastAltitude
            if (abs(altDiff) > ALTITUDE_THRESHOLD) {
                lastAltitude += altDiff / 2.0f

                val altitudeOffset = lastAltitude - initialAltitude
                val rounded = (altitudeOffset * ALTITUDE_DECIMALS).roundToInt() / ALTITUDE_DECIMALS
                altitudeState.value = rounded
            }
        }
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val w = event.values[3]
            val deviceRotation = Quat(x, y, z, w)
            val previousRotation = deviceRotationState.value
            val rotDiff = rotationDifference(deviceRotation, previousRotation)
            if (rotDiff > 0.05f) {
                deviceRotationState.value = deviceRotation
                val orientationVals = deviceRotation.eulerAngles()
                this.azimuthRot = Math.toDegrees(-orientationVals[0].toDouble()).toFloat()
            }
        }
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
        }
        updateAzimuth()

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    private fun rotationDifference(q1: Quat, q2: Quat): Float {
        val dotProduct = q1.dot(q2)
        val angle = 2.0f * acos(abs(dotProduct))
        return angle
    }

    private fun updateAzimuth() {
        if (gravity != null && geomagnetic != null) {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)
            val success = SensorManager.getRotationMatrix(
                rotationMatrix, inclinationMatrix, gravity, geomagnetic
            )
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() // Azimuth in degrees
            }
        }
    }

    private fun updateLocation(location: Location) {
        if (initialLocation == null) {
            initialLocation = location
//            fetchWeatherData(location)
        }
        toast("Loc $location", silent = true)
        this.locationState.value = location;
        this.lastLocation = location
    }

    private fun fetchWeatherData(location: Location) {
        callAsync(
            lifecycleScope,
            { fetchWeatherData(location.latitude, location.longitude) },
            { jsonResponse -> onWeatherData(jsonResponse) },
            { errorMessage -> toast("Error fetching weather data: ${errorMessage}") }
        )
    }

    private fun pressureToAltitude(pressure: Float): Float {
        val seaLevelPressure = 1013.25f
        val exponentBase = 0.190263
        val altitudeScale = 44330.8f
        val exponent = (pressure / seaLevelPressure).toDouble().let { Math.pow(it, exponentBase) }
        return (1 - exponent).toFloat() * altitudeScale
    }

    fun resetAltitude() {
        if (initialAltitudeSet) {
            altitudeState.value = 0.0f
            initialAltitude = lastAltitude
        }
    }

    fun resetLocation() {
        initialLocation = lastLocation
    }

    fun toast(text: String, silent: Boolean = false) {
        Log.d("LOG", text)
        if (!silent) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastToastTime >= 0) {
                Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                lastToastTime = currentTime
            }
        }
    }

    fun vibrate() {
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                10, VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }

    fun getElapsedSecs(): Float {
        val elapsedMs = System.currentTimeMillis() - initialTime
        return elapsedMs / 1000.0f;
    }

}