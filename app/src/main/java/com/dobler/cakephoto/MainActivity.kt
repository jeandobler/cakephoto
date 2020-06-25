package com.dobler.cakephoto

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: BarcodeScanner
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private lateinit var backCamera: String
    private lateinit var currentCameraDevice: CameraDevice
    var detectingImage = false
    var capture = false

    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE
        ).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupManagers()
        startCamera()

        btCapture.setOnClickListener {
            changeCapturingButton(null)
        }


    }

    fun changeCapturingButton(boolean: Boolean?) {
        capture = if (boolean != null) boolean else !capture
        if (capture) {
            btCapture.text = "Stop Capturing"
            scanner = BarcodeScanning.getClient(options)

        } else {
            btCapture.text = "Capture"
            scanner.close()
        }
    }


    private fun startCamera() {
        tvCameraPreview.setSurfaceTextureListener(surfaceReadyCallback)
    }

    lateinit var cameraManager: CameraManager
    private fun setupManagers() {
        cameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
    }


    private fun getRotationCompensation(
        cameraId: String,
        activity: Activity,
        context: Context
    ): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        return rotationCompensation
    }

    val surfaceReadyCallback = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            if (!detectingImage) {
                if (capture) {
                    Log.e("Image", detectingImage.toString())
//                    runBlocking  {
                    GlobalScope.launch {
                        detectImage()
                    }
                }
            }
        }

        suspend fun detectImage() = withContext(Dispatchers.IO) {
            detectingImage = true
            val mediaImage = InputImage.fromBitmap(
                tvCameraPreview.bitmap,
                getRotationCompensation(
                    backCamera,
                    this@MainActivity,
                    this@MainActivity
                )
            )

            scanner.process(mediaImage)
                .addOnSuccessListener { barcodes ->

                    for (barcode in barcodes) {
                        val intent = Intent(
                            this@MainActivity,
                            ResultActivity::class.java
                        ).apply {
                            putExtra("barcode", barcode.rawValue)
                        }
                        startActivity(intent)
                        scanner.close()
                        changeCapturingButton(false)
                        break
                    }

                }
            Thread.sleep(1500)
            detectingImage = false
        }


        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return true
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            getBackCamera()

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            cameraManager.openCamera(backCamera, stateCallback, Handler { true })
        }

    }

    private fun getBackCamera() {
        if (cameraManager.cameraIdList.isEmpty()) {
            return
        }

        cameraManager.cameraIdList.forEach { foundCamera ->
            cameraCharacteristics = cameraManager.getCameraCharacteristics(foundCamera)

            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                backCamera = foundCamera
            }
        }
    }

    private lateinit var previewSurface: Surface //Apenas se usar TextureView
    val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            if (cameraDevice != null) {
                currentCameraDevice = cameraDevice
            }
            // Para a imagem do preview não ficar distorcida precisamos pegar os tipos de resolução
            // que a camera suporta, para isso usaremos o Camera Characteristics
            cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let { streamConfigurationMap ->
                streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)
                    ?.let { yuvSizes ->
                        // A primcire resolução no meu caso é a mais alta mas você pode debugar
                        // caso não saiba qual é a melhor resolução
                        val previewSize = yuvSizes.first()

                        // Aqui adicionarmos o tamanho da resolução como default
                        tvCameraPreview.surfaceTexture.setDefaultBufferSize(
                            previewSize.width, previewSize.height
                        )
                        //Chamamos o adaptScreen para ajustar a view caso necessário

                        adaptScreen()
                        previewSurface =
                            Surface(tvCameraPreview.surfaceTexture) //Apenas se usar Textureview

                        cameraDevice.createCaptureSession(
                            mutableListOf(previewSurface),
//                            mutableListOf(tvCameraPreview.holder.surface) //Substituir caso vá usar SurfaceView
                            captureCallback,
                            Handler { true }
                        )

                    }
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
        }

        override fun onError(camera: CameraDevice, error: Int) {
        }
    }

    fun adaptScreen() {
        val cameraAspectRatio = 0.75.toFloat()

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        var finalWidth = screenWidth
        var finalHeight = screenHeight
        val screenAspectRatio = screenWidth.toFloat() / screenHeight

        if (screenAspectRatio > cameraAspectRatio) {
            finalHeight = (screenWidth / cameraAspectRatio).toInt()
        } else {
            finalWidth = (screenHeight * cameraAspectRatio).toInt()
        }

        val lp = tvCameraPreview.layoutParams

        lp.width = finalWidth
        lp.height = finalHeight
        tvCameraPreview.setLayoutParams(lp)
    }

    val captureCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {}

        override fun onConfigured(session: CameraCaptureSession) {
            val previewRequestBuilder =
                currentCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    .apply {
                        addTarget(previewSurface)
                        set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )
                    }

            session.setRepeatingRequest(
                previewRequestBuilder.build(),
                object : CameraCaptureSession.CaptureCallback() {},
                Handler { true }
            )

        }
    }


}