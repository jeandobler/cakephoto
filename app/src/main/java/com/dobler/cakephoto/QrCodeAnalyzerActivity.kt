package com.dobler.cakephoto

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

internal class QrCodeAnalyzer(private val luma: (str: String?) -> Unit) : ImageAnalysis.Analyzer {
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE
        ).build()

    private var scanner: BarcodeScanner by lazy {
        BarcodeScanning.getClient(options)
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        val image = imageProxy.image
        if (image != null) {
            val mediaImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            scanner.process(mediaImage)
                .addOnSuccessListener { barcodes ->

                    for (barcode in barcodes) {

                        luma(barcode.rawValue)
                        scanner.close()
                        break
                    }

                }
        }
    }
}