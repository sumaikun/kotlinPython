package com.example.testpython

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import java.lang.Math.abs


class CameraHelper{
    private var cameraDevice: CameraDevice? = null
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var previewSurface: Surface
    private var cameraId: String? = null
    private lateinit var textureView: TextureView

    @Throws(CameraAccessException::class)
    fun startCamera(context: Context, surfaceTexture: SurfaceTexture, imageReaderSurface: Surface, textureView: TextureView) {
        this.textureView = textureView
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = manager.cameraIdList
        for (id in cameraIds) {
            val cameraCharacteristics = manager.getCameraCharacteristics(id)
            val cameraDirection = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection == CameraCharacteristics.LENS_FACING_BACK) {
                cameraId = id
                break
            }
        }

        val cameraCharacteristics = manager.getCameraCharacteristics(cameraId!!)
        val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        // Select the optimal preview size
        val previewSize = streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java)?.let { chooseOptimalSize(it, textureView.width, textureView.height) }

        // Set the aspect ratio of the texture view to match the preview size
        if (previewSize != null) {
            val aspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()
            setAspectRatio(textureView, aspectRatio)
        }

        // Open the camera
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera

                // Create a preview session
                val surfaces = listOf(
                    Surface(surfaceTexture),
                    imageReaderSurface
                )

                camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        previewRequestBuilder.addTarget(surfaces[0])
                        previewRequestBuilder.addTarget(surfaces[1])

                        // Create a repeating request for the preview
                        session.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("CameraError", "Failed to configure camera session.")
                    }
                }, null)
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice?.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }, null)
    }

    fun stopCamera() {
        captureSession.close()
        cameraDevice?.close()
        cameraDevice = null
    }

    private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size? {
        val aspectRatio = Rational(width, height)
        var optimalSize: Size? = null
        var minDiff = Int.MAX_VALUE
        for (size in choices) {
            val ratio = Rational(size.width, size.height)
            if (abs(ratio.toFloat() - aspectRatio.toFloat()) < minDiff) {
                optimalSize = size
                minDiff = abs(ratio.toFloat() - aspectRatio.toFloat()).toInt()
            }
        }
        return optimalSize
    }

    fun setAspectRatio(textureView: TextureView, aspectRatio: Float) {
        textureView.post {
            val layoutParams = textureView.layoutParams
            layoutParams.width = textureView.width
            layoutParams.height = (textureView.width * aspectRatio).toInt()
            textureView.layoutParams = layoutParams
        }
    }
}
