package com.example.ui.noorup

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGlow
import kotlin.math.*

@Composable
fun QiblaCompassView(
    viewModel: NoorUpViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isEnglish by viewModel.isEnglish.collectAsState()
    val cityLocation by viewModel.selectedCityLocation.collectAsState()

    // Compute Qibla mathematical metrics for current location
    val qiblaInfo = remember(cityLocation.latitude, cityLocation.longitude) {
        SolarPrayerEngine.calculateQibla(cityLocation.latitude, cityLocation.longitude)
    }

    // Live Sensor States
    var rawAzimuth by remember { mutableFloatStateOf(0f) }
    var rawPitch by remember { mutableFloatStateOf(0f) }
    var rawRoll by remember { mutableFloatStateOf(0f) }
    var isSensorAvailable by remember { mutableStateOf(true) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var manualBearingOffset by remember { mutableFloatStateOf(0f) }
    var isManualMode by remember { mutableStateOf(false) }

    // Sensor Fusion Listener
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        var registered = false

        if (sensorManager != null) {
            val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)
            val lastAccelerometer = FloatArray(3)
            val lastMagnetometer = FloatArray(3)
            var lastAccelerometerSet = false
            var lastMagnetometerSet = false

            val sensorEventListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return

                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientationValues)
                            val azimuthRad = orientationValues[0]
                            val pitchRad = orientationValues[1]
                            val rollRad = orientationValues[2]

                            var deg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                            deg = (deg + 360f) % 360f

                            rawAzimuth = deg
                            rawPitch = Math.toDegrees(pitchRad.toDouble()).toFloat()
                            rawRoll = Math.toDegrees(rollRad.toDouble()).toFloat()
                            isSensorAvailable = true
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                            lastAccelerometerSet = true
                            if (lastAccelerometerSet && lastMagnetometerSet) {
                                computeOrientation(rotationMatrix, orientationValues, lastAccelerometer, lastMagnetometer)
                            }
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                            lastMagnetometerSet = true
                            if (lastAccelerometerSet && lastMagnetometerSet) {
                                computeOrientation(rotationMatrix, orientationValues, lastAccelerometer, lastMagnetometer)
                            }
                        }
                        Sensor.TYPE_ORIENTATION -> {
                            var deg = event.values[0]
                            deg = (deg + 360f) % 360f
                            rawAzimuth = deg
                            rawPitch = event.values[1]
                            rawRoll = event.values[2]
                            isSensorAvailable = true
                        }
                    }
                }

                private fun computeOrientation(
                    rMatrix: FloatArray,
                    oVals: FloatArray,
                    acc: FloatArray,
                    mag: FloatArray
                ) {
                    val success = SensorManager.getRotationMatrix(rMatrix, null, acc, mag)
                    if (success) {
                        SensorManager.getOrientation(rMatrix, oVals)
                        var deg = Math.toDegrees(oVals[0].toDouble()).toFloat()
                        deg = (deg + 360f) % 360f
                        rawAzimuth = deg
                        rawPitch = Math.toDegrees(oVals[1].toDouble()).toFloat()
                        rawRoll = Math.toDegrees(oVals[2].toDouble()).toFloat()
                        isSensorAvailable = true
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    sensorAccuracy = accuracy
                }
            }

            if (rotationVectorSensor != null) {
                registered = sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            } else if (accelerometer != null && magnetometer != null) {
                val r1 = sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                val r2 = sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
                registered = r1 && r2
            } else if (orientationSensor != null) {
                registered = sensorManager.registerListener(sensorEventListener, orientationSensor, SensorManager.SENSOR_DELAY_UI)
            }

            if (!registered) {
                isSensorAvailable = false
            }

            onDispose {
                sensorManager.unregisterListener(sensorEventListener)
            }
        } else {
            isSensorAvailable = false
            onDispose {}
        }
    }

    // Smooth Azimuth calculation with angular interpolation
    var currentContinuousAzimuth by remember { mutableFloatStateOf(0f) }
    val effectiveAzimuth = if (isManualMode || !isSensorAvailable) manualBearingOffset else rawAzimuth

    LaunchedEffect(effectiveAzimuth) {
        val diff = (effectiveAzimuth - (currentContinuousAzimuth % 360f) + 540f) % 360f - 180f
        currentContinuousAzimuth += diff
    }

    val animatedContinuousAzimuth by animateFloatAsState(
        targetValue = currentContinuousAzimuth,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "compassAzimuth"
    )

    val currentHeadingDeg = (animatedContinuousAzimuth % 360f + 360f) % 360f

    // Angle from phone forward (top) to Kaaba
    // When phone points north (heading=0), Kaaba is at qiblaInfo.bearingDegrees
    // Needle relative angle = (qiblaBearing - currentHeadingDeg)
    val relativeQiblaAngle = (qiblaInfo.bearingDegrees.toFloat() - currentHeadingDeg + 360f) % 360f
    val shortestAngleDiff = abs((qiblaInfo.bearingDegrees.toFloat() - currentHeadingDeg + 540f) % 360f - 180f)

    val isAligned = shortestAngleDiff <= 4.0f
    val isTilted = abs(rawPitch) > 35f || abs(rawRoll) > 35f

    // Haptic feedback trigger on entering aligned state
    var prevAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !prevAligned) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            } catch (e: Exception) {
                // Ignore vibration errors
            }
        }
        prevAligned = isAligned
    }

    // Pulse animation for aligned state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val dialBorderColor by animateColorAsState(
        targetValue = if (isAligned) EmeraldGlow else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        label = "dialBorder"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEnglish) "Qibla Direction (কিবলা কম্পাস)" else "কিবলা কম্পাস (Qibla Compass)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (isEnglish) cityLocation.nameEn else cityLocation.nameBn} • ${String.format("%.2f°N, %.2f°E", cityLocation.latitude, cityLocation.longitude)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    // Alignment Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAligned) EmeraldGlow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAligned) EmeraldGlow else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isAligned) EmeraldGlow else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isAligned) (if (isEnglish) "ALIGNED" else "সংলগ্ন") else "${shortestAngleDiff.toInt()}°",
                                color = if (isAligned) EmeraldGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Key Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Qibla Angle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "Qibla Angle" else "কাবার কোণ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${String.format("%.1f°", qiblaInfo.bearingDegrees)}",
                            color = EmeraldGlow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) qiblaInfo.cardinalDirectionEn else qiblaInfo.cardinalDirectionBn,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )

                    // Phone Heading
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "Phone Heading" else "ফোনের দিক",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${currentHeadingDeg.toInt()}°",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getHeadingCardinal(currentHeadingDeg, isEnglish),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )

                    // Distance to Makkah
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "Makkah Distance" else "মক্কার দূরত্ব",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${qiblaInfo.distanceKm.toInt()} km",
                            color = Color(0xFFFBBF24),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) "Holy Kaaba" else "পবিত্র কাবা শরীফ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tilted Warning if phone is held upright instead of flat
        if (isTilted && isSensorAvailable && !isManualMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0x33F59E0B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ScreenRotation,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) "Hold phone flat (parallel to ground) for highest precision" else "সর্বোচ্চ নির্ভুলতার জন্য ফোনটি সমতল পৃষ্ঠে বা সোজা রাখুন",
                        color = Color(0xFFFCD34D),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Live Interactive Compass Dial
        Box(
            modifier = Modifier
                .size(280.dp)
                .shadow(
                    elevation = if (isAligned) (16.dp * pulseGlow) else 4.dp,
                    shape = CircleShape,
                    ambientColor = if (isAligned) EmeraldGlow else Color.Transparent,
                    spotColor = if (isAligned) EmeraldGlow else Color.Transparent
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(
                    width = if (isAligned) 3.5.dp else 2.dp,
                    brush = if (isAligned) {
                        Brush.sweepGradient(listOf(EmeraldGlow, Color(0xFF6EE7B7), EmeraldGlow))
                    } else {
                        SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Compass Rose Dial Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-animatedContinuousAzimuth)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Outer decorative concentric rings
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = radius * 0.88f,
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = radius * 0.72f,
                    style = Stroke(width = 1f)
                )

                // 360 Degree Ticks
                for (degree in 0 until 360 step 5) {
                    val angleRad = Math.toRadians(degree.toDouble())
                    val isMajor = degree % 30 == 0
                    val isCardinal = degree % 90 == 0
                    val tickLen = when {
                        isCardinal -> 14.dp.toPx()
                        isMajor -> 9.dp.toPx()
                        else -> 5.dp.toPx()
                    }
                    val strokeW = when {
                        isCardinal -> 2.5f
                        isMajor -> 1.8f
                        else -> 1.0f
                    }
                    val tickColor = when {
                        degree == 0 -> Color(0xFFEF4444) // North in Red
                        isCardinal -> Color.White.copy(alpha = 0.9f)
                        isMajor -> Color.White.copy(alpha = 0.6f)
                        else -> Color.White.copy(alpha = 0.25f)
                    }

                    val startX = center.x + (radius - 8.dp.toPx()) * sin(angleRad).toFloat()
                    val startY = center.y - (radius - 8.dp.toPx()) * cos(angleRad).toFloat()
                    val endX = center.x + (radius - 8.dp.toPx() - tickLen) * sin(angleRad).toFloat()
                    val endY = center.y - (radius - 8.dp.toPx() - tickLen) * cos(angleRad).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeW
                    )
                }

                // Draw Kaaba Marker on the rotating dial at the exact Qibla Bearing
                val qiblaRad = Math.toRadians(qiblaInfo.bearingDegrees.toDouble())
                val kaabaDist = radius * 0.78f
                val kaabaCenterX = center.x + kaabaDist * sin(qiblaRad).toFloat()
                val kaabaCenterY = center.y - kaabaDist * cos(qiblaRad).toFloat()

                // Golden beacon line from center towards Kaaba
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0xFFF59E0B).copy(alpha = 0.8f))
                    ),
                    start = center,
                    end = Offset(kaabaCenterX, kaabaCenterY),
                    strokeWidth = 2.5f
                )

                // Kaaba marker disc
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = 14.dp.toPx(),
                    center = Offset(kaabaCenterX, kaabaCenterY)
                )
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = 14.dp.toPx(),
                    center = Offset(kaabaCenterX, kaabaCenterY),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Inner Kaaba golden cube outline
                val cubeSize = 10.dp.toPx()
                drawRect(
                    color = Color(0xFFFBBF24),
                    topLeft = Offset(kaabaCenterX - cubeSize / 2f, kaabaCenterY - cubeSize / 2f),
                    size = androidx.compose.ui.geometry.Size(cubeSize, cubeSize),
                    style = Stroke(width = 2f)
                )
            }

            // Fixed Cardinal Labels Overlay (N, E, S, W) rotating with dial
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-animatedContinuousAzimuth)
            ) {
                // North (Top)
                Text(
                    text = "N",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp)
                )
                // East (Right)
                Text(
                    text = "E",
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp)
                )
                // South (Bottom)
                Text(
                    text = "S",
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                )
                // West (Left)
                Text(
                    text = "W",
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 18.dp)
                )
            }

            // Central Needle pointing directly to Kaaba from top of the phone
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(relativeQiblaAngle),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val needleLen = size.width * 0.36f

                    // Triangle path for top Qibla Arrow
                    val path = Path().apply {
                        moveTo(center.x, center.y - needleLen)
                        lineTo(center.x - 12.dp.toPx(), center.y - 12.dp.toPx())
                        lineTo(center.x, center.y)
                        lineTo(center.x + 12.dp.toPx(), center.y - 12.dp.toPx())
                        close()
                    }

                    // Top arrow (Emerald glow or Green)
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = if (isAligned) listOf(Color(0xFF34D399), EmeraldGlow) else listOf(EmeraldGlow, Color(0xFF047857))
                        )
                    )

                    // Bottom Tail (Subtle grey indicator)
                    val tailPath = Path().apply {
                        moveTo(center.x, center.y + needleLen * 0.45f)
                        lineTo(center.x - 8.dp.toPx(), center.y + 10.dp.toPx())
                        lineTo(center.x, center.y)
                        lineTo(center.x + 8.dp.toPx(), center.y + 10.dp.toPx())
                        close()
                    }
                    drawPath(
                        path = tailPath,
                        color = Color.White.copy(alpha = 0.25f)
                    )
                }

                // Kaaba Icon badge right above the needle tip
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 46.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.5.dp, if (isAligned) EmeraldGlow else Color(0xFFFBBF24), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🕋", fontSize = 14.sp)
                    }
                }
            }

            // Center Pin / Pivot
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAligned) EmeraldGlow else MaterialTheme.colorScheme.surface
                    )
                    .border(2.dp, if (isAligned) Color.White else EmeraldGlow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isAligned) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Aligned",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = EmeraldGlow,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(relativeQiblaAngle)
                    )
                }
            }

            // Top Device Heading Marker (Fixed notch at 12 o'clock of the phone)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .size(width = 16.dp, height = 8.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(if (isAligned) EmeraldGlow else Color(0xFFEF4444))
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Real-Time Guidance Direction Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isAligned) EmeraldGlow.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isAligned) EmeraldGlow else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Explore,
                    contentDescription = null,
                    tint = if (isAligned) EmeraldGlow else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (isAligned) {
                        Text(
                            text = if (isEnglish) "✨ Perfectly Aligned with Kaaba!" else "✨ কাবার দিকে নির্ভুলভাবে সংলগ্ন!",
                            color = EmeraldGlow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) "You are now facing the Holy Kaaba directly." else "আপনি এখন সরাসরি পবিত্র কাবা শরীফের মুখোমুখি আছেন।",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    } else {
                        val turnLeft = ((qiblaInfo.bearingDegrees - currentHeadingDeg + 360) % 360) > 180
                        val turnDeg = shortestAngleDiff.toInt()
                        Text(
                            text = if (isEnglish) {
                                "Turn phone ${if (turnLeft) "Left" else "Right"} by $turnDeg°"
                            } else {
                                "ফোনটি ${if (turnLeft) "বামে" else "ডানে"} $turnDeg° ঘুরান"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnglish) "Align the green arrow with the top marker" else "সবুজ তীরচিহ্নটি ফোনের শীর্ষে নিয়ে আসুন",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Manual Angle / Emulator Simulation Control (in case user device lacks magnetometer or wants to test)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isManualMode = !isManualMode }
            ) {
                Icon(
                    imageVector = if (isSensorAvailable) Icons.Default.Sensors else Icons.Default.SensorsOff,
                    contentDescription = null,
                    tint = if (isSensorAvailable && !isManualMode) EmeraldGlow else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isManualMode) {
                        if (isEnglish) "Manual Mode (Active)" else "ম্যানুয়াল মোড চালু"
                    } else if (isSensorAvailable) {
                        if (isEnglish) "Live Sensor Active" else "লাইভ সেন্সর সক্রিয়"
                    } else {
                        if (isEnglish) "Sensor Not Found (Manual Active)" else "সেন্সর পাওয়া যায়নি (ম্যানুয়াল চালু)"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            TextButton(
                onClick = { isManualMode = !isManualMode },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isManualMode) (if (isEnglish) "Use Sensor" else "সেন্সর ব্যবহার করুন") else (if (isEnglish) "Test Slider" else "স্লাইডার টেস্ট"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isManualMode || !isSensorAvailable) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    text = if (isEnglish) "Rotate Dial Manually: ${manualBearingOffset.toInt()}°" else "ম্যানুয়ালি ডায়াল ঘুরান: ${manualBearingOffset.toInt()}°",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Slider(
                    value = manualBearingOffset,
                    onValueChange = { manualBearingOffset = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldGlow,
                        activeTrackColor = EmeraldGlow
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Calibration Tips Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AllInclusive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEnglish) {
                        "Tip: If direction drifts, wave your phone in a figure-8 motion (∞) to recalibrate the magnetic compass."
                    } else {
                        "পরামর্শ: কম্পাস দিক পরিবর্তন না হলে ফোনটিকে বাতাসে ৮ (∞) আকৃতিতে কয়েকবার ঘুরিয়ে ক্যালিব্রেট করুন।"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

private fun getHeadingCardinal(heading: Float, isEnglish: Boolean): String {
    val norm = (heading % 360f + 360f) % 360f
    return when {
        norm >= 337.5f || norm < 22.5f -> if (isEnglish) "North (N)" else "উত্তর (N)"
        norm < 67.5f -> if (isEnglish) "Northeast (NE)" else "উত্তর-পূর্ব (NE)"
        norm < 112.5f -> if (isEnglish) "East (E)" else "পূর্ব (E)"
        norm < 157.5f -> if (isEnglish) "Southeast (SE)" else "দক্ষিণ-পূর্ব (SE)"
        norm < 202.5f -> if (isEnglish) "South (S)" else "দক্ষিণ (S)"
        norm < 247.5f -> if (isEnglish) "Southwest (SW)" else "দক্ষিণ-পশ্চিম (SW)"
        norm < 292.5f -> if (isEnglish) "West (W)" else "পশ্চিম (W)"
        else -> if (isEnglish) "Northwest (NW)" else "উত্তর-পশ্চিম (NW)"
    }
}
