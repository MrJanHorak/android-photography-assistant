package com.janhorak.shutterdeck.ui.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun BackCameraPreview(
    modifier: Modifier = Modifier,
    onExposureSample: ((aperture: Float, shutterSeconds: Double, iso: Int) -> Unit)? = null,
    onBindingError: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnExposureSample = rememberUpdatedState(onExposureSample)
    val latestOnBindingError = rememberUpdatedState(onBindingError)
    val previewView = remember {
        PreviewView(context).apply {
            // COMPATIBLE keeps the preview in the normal view hierarchy so Compose Canvas overlays
            // render above it correctly. PERFORMANCE uses SurfaceView and will hide the overlay.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(context, lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var cameraProvider: ProcessCameraProvider? = null
        var boundPreview: Preview? = null
        var disposed = false

        val listener = Runnable {
            if (disposed) return@Runnable

            try {
                cameraProvider = cameraProviderFuture.get()

                val previewBuilder = Preview.Builder()
                if (latestOnExposureSample.value != null) {
                    Camera2Interop.Extender(previewBuilder)
                        .setSessionCaptureCallback(
                            object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(
                                    session: CameraCaptureSession,
                                    request: android.hardware.camera2.CaptureRequest,
                                    result: TotalCaptureResult,
                                ) {
                                    val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
                                    val exposureTimeNanos = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
                                    val aperture = result.get(CaptureResult.LENS_APERTURE)

                                    if (iso != null && exposureTimeNanos != null && aperture != null) {
                                        latestOnExposureSample.value?.invoke(
                                            aperture,
                                            exposureTimeNanos / 1_000_000_000.0,
                                            iso,
                                        )
                                    }
                                }
                            },
                        )
                }

                val preview = previewBuilder.build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                boundPreview?.let { existingPreview ->
                    cameraProvider?.unbind(existingPreview)
                }
                boundPreview = preview
                cameraProvider?.unbind(preview)
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                )
            } catch (_: Exception) {
                latestOnBindingError.value?.invoke()
            }
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            disposed = true
            boundPreview?.let { preview ->
                cameraProvider?.unbind(preview)
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
