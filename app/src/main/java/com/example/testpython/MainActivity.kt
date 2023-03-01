package com.example.testpython

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var textureView: TextureView
    private lateinit var imageReader: ImageReader
    private var cameraHelper = CameraHelper()

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        textureView.surfaceTextureListener = this

        // Check for camera permission
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {

        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Initialize Python interpreter
        val py = Python.getInstance()

        // Load the Python module
        val hello = py.getModule("hello")

        // Initialize image reader
        imageReader = ImageReader.newInstance(textureView.width, textureView.height, ImageFormat.JPEG, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                // Convert the image to a Bitmap
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                image.close()

                // Convert the bitmap to a byte array
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val bitmapBytes = outputStream.toByteArray()
                // Convert the byte array to a numpy array


                GlobalScope.launch {
                    val np = py.getModule("numpy")
                    val bitmapArray = np.callAttr("array", bitmapBytes)
                    hello.callAttr("process_frame", bitmapArray)
                }
            }
        }, Handler(Looper.getMainLooper()))

        // Start camera
        cameraHelper.startCamera(this, surface, imageReader.surface, textureView)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // Not implemented
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // Stop camera
        cameraHelper.stopCamera()

        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Not implemented
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                textureView.surfaceTextureListener = this
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            }
        }
    }
}
