package com.example.hellosky.presentation

import ARROW_MESH
import MeshView
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.example.hellosky.ui.theme.HelloSkyTheme
import distanceBetweenLocations
import glm_.vec3.Vec3
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }

    @Composable
    fun WearApp() {
        HelloSkyTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {

                    if (true) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = min(centerX, centerY) - 10  // Adjust to keep dot on the edge

                        val adjustedAzimuth = -azimuthRot - 90
                        val azimuthRadians = Math.toRadians(adjustedAzimuth.toDouble())

                        val radiansRot = deviceRotationState.value.eulerAngles() // Vec3 in radians
                        val rotDegrees = Vec3(
                            Math.toDegrees(radiansRot.x.toDouble()).toFloat(),
                            Math.toDegrees(radiansRot.y.toDouble()).toFloat(),
                            Math.toDegrees(radiansRot.z.toDouble()).toFloat()
                        )

                        var arrowX = radius * cos(azimuthRadians).toFloat()
                        var arrowY = radius * sin(azimuthRadians).toFloat()

                        if (rotDegrees.z > -90 && rotDegrees.z < 90) {
                            arrowY *= -1
                        }

                        var dotX = centerX + arrowX
                        var dotY = centerY + arrowY

                        // Draw a red dot
                        drawCircle(
                            color = Color.Red, radius = 20f,  // Adjust dot size as needed
                            center = Offset(dotX, dotY)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MeshView(ARROW_MESH, deviceRotationState.value, drawEdges = false)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AltitudeDisplay()
                    LocationDisplay()
                    ResetButton()
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun ResetButton() {
        var isPressed by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .height(20.dp)
                .pointerInteropFilter {
                    when (it.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isPressed = true
                            coroutineScope.launch {
                                delay(1500)
                                if (isPressed) {
                                    resetAltitude()
                                    resetLocation()
                                    toast("Location Reset")
                                    vibrate()
                                }
                            }
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isPressed = false
                        }
                    }
                    true
                }
        ) {
            Text(text = "Reset")
        }
    }

    // ViewModel for time
    class TimeViewModel : ViewModel() {
        private val _currentTime = MutableLiveData<String>()
        val currentTime: LiveData<String> get() = _currentTime

        init {
            updateTime()
        }

        private fun updateTime() {
            viewModelScope.launch {
                while (true) {
                    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val time = formatter.format(Date())
                    _currentTime.value = time
                    delay(1000) // Update every second
                }
            }
        }
    }

    val timeViewModel = TimeViewModel()

    @Composable
    fun LocationDisplay() {
        val location = locationState.value
        val currentTime by timeViewModel.currentTime.observeAsState("")

        var altitudeStr = "???"
        var distanceTo = 0f

        if (this.initialLocation != null) {
            val initialLocation = this.initialLocation!!

            // Distance to
            distanceTo = distanceBetweenLocations(
                location.latitude,
                location.longitude,
                initialLocation.latitude,
                initialLocation.longitude
            )

            // Altitude
            if (location.hasAltitude()) {
                val altitude = initialLocation.altitude
                altitudeStr = String.format("%.1f", altitude)
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            text = String.format(
                Locale.getDefault(),
                "%s\nBattery: %d, %s/h",
                currentTime,
                getBatteryLevel(),
                batteryDrainPerHourState.value
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    @Composable
    fun AltitudeDisplay() {
        val altitude = altitudeState.value;
        Text(
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = String.format("%.1f m", altitude),
            fontSize = 50.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    @Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreview() {
        WearApp()
    }
}