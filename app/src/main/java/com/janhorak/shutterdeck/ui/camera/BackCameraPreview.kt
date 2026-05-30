package com.janhorak.shutterdeck.ui.camera

import android.util.Rational
import android.util.Size
import android.view.Surface
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
internal fun BackCameraPreview(
    modifier: Modifier = Modifier,
    onExposureSample: ((aperture: Float, shutterSeconds: Double, iso: Int) -> Unit)? = null,
    imageAnalyzer: ImageAnalysis.Analyzer? = null,
    analysisTargetResolution: Size = Size(640, 480),
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

    DisposableEffect(context, lifecycleOwner, previewView, imageAnalyzer, analysisTargetResolution) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor: ExecutorService? = if (imageAnalyzer != null) {
            Executors.newSingleThreadExecutor()
        } else {
            null
        }
        var cameraProvider: ProcessCameraProvider? = null
        var boundPreview: Preview? = null
        var boundAnalysis: ImageAnalysis? = null
        var disposed = false
        var lastBoundWidth = -1
        var lastBoundHeight = -1
        var lastBoundRotation = -1
        var lastBoundWithAnalysis = false

        fun clearBoundUseCases() {
            boundAnalysis?.clearAnalyzer()
            boundPreview?.let { preview ->
                cameraProvider?.unbind(preview)
            }
            boundAnalysis?.let { analysis ->
                cameraProvider?.unbind(analysis)
            }
            boundPreview = null
            boundAnalysis = null
        }

        fun bindCameraIfReady() {
            if (disposed) return

            val provider = cameraProvider ?: return
            val previewWidth = previewView.width
            val previewHeight = previewView.height
            if (previewWidth <= 0 || previewHeight <= 0) return

            val displayRotation = previewView.display?.rotation ?: Surface.ROTATION_0
            val hasAnalysis = imageAnalyzer != null
            if (
                boundPreview != null &&
                lastBoundWidth == previewWidth &&
                lastBoundHeight == previewHeight &&
                lastBoundRotation == displayRotation &&
                lastBoundWithAnalysis == hasAnalysis
            ) {
                return
            }

            try {
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

                val analysis = imageAnalyzer?.let { analyzer ->
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(analysisTargetResolution)
                        .build()
                        .apply {
                            setAnalyzer(analysisExecutor!!, analyzer)
                        }
                }

                clearBoundUseCases()

                boundPreview = preview
                boundAnalysis = analysis

                if (analysis != null) {
                    val viewport = ViewPort.Builder(
                        Rational(previewWidth, previewHeight),
                        displayRotation,
                    )
                        .setScaleType(ViewPort.FILL_CENTER)
                        .build()
                    val useCaseGroup = UseCaseGroup.Builder()
                        .setViewPort(viewport)
                        .addUseCase(preview)
                        .addUseCase(analysis)
                        .build()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        useCaseGroup,
                    )
                } else {
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                    )
                }

                lastBoundWidth = previewWidth
                lastBoundHeight = previewHeight
                lastBoundRotation = displayRotation
                lastBoundWithAnalysis = analysis != null
            } catch (_: Exception) {
                latestOnBindingError.value?.invoke()
            }
        }

        val layoutChangeListener = android.view.View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            bindCameraIfReady()
        }
        previewView.addOnLayoutChangeListener(layoutChangeListener)

        val listener = Runnable {
            if (disposed) return@Runnable

            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraIfReady()
            } catch (_: Exception) {
                latestOnBindingError.value?.invoke()
            }
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            disposed = true
            previewView.removeOnLayoutChangeListener(layoutChangeListener)
            clearBoundUseCases()
            analysisExecutor?.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
