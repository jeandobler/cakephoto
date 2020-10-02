package com.dobler.cakephoto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_camerax.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXActivity : BaseActivity() {
//    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camerax)
        initCameraPermission()

        // Set up the listener for take photo button
        // camera_capture_button.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    override fun onStart() {
        super.onStart()
        startCamera()
    }

    override fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            //Analyser
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrCodeAnalyzer {
                        val intent = Intent(
                            this,
                            ResultActivity::class.java
                        ).apply {
                            putExtra("barcode", it)
                        }
                        startActivity(intent)
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
//                    , imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e("Result", exc.message)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}
