package com.example.photography_helper.metering.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale

@Composable
fun LightMeterScreen(
    modifier: Modifier = Modifier,
    viewModel: LightMeterViewModel = hiltViewModel()
) {
    val evValue by viewModel.evState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var aperture by remember { mutableStateOf(2.8f) }
    var iso by remember { mutableStateOf(100) }
    var calibrationOffset by remember { mutableStateOf(0f) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startMetering()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopMetering()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopMetering()
        }
    }

    LaunchedEffect(calibrationOffset) {
        viewModel.setCalibrationOffset(calibrationOffset)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Light Meter",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = String.format(Locale.getDefault(), "Measured EV: %.2f", evValue),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val requiredShutter = viewModel.calculateShutterSpeed(aperture, iso, evValue)
        val shutterStr = if (requiredShutter < 1.0) {
            String.format(Locale.getDefault(), "1/%.0f s", 1.0 / requiredShutter)
        } else {
            String.format(Locale.getDefault(), "%.1f s", requiredShutter)
        }

        Text(
            text = "Suggested Shutter: $shutterStr",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Aperture and ISO controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Aperture: f/$aperture")
                Slider(
                    value = aperture,
                    onValueChange = { aperture = it },
                    valueRange = 1.4f..22f,
                    modifier = Modifier.width(120.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ISO: $iso")
                Slider(
                    value = iso.toFloat(),
                    onValueChange = { iso = it.toInt() },
                    valueRange = 50f..6400f,
                    modifier = Modifier.width(120.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Calibration Offset: ${String.format(Locale.getDefault(), "%+.1f EV", calibrationOffset)}")
            Slider(
                value = calibrationOffset,
                onValueChange = { calibrationOffset = it },
                valueRange = -3f..3f,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}