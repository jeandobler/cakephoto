package com.dobler.cakephoto

import android.annotation.SuppressLint
import android.util.Log
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
    val scanner: BarcodeScanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        Log.e("Scanned", "Analyzing")
        val image = imageProxy.image
        if (image != null) {
            val mediaImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            Log.e("Scanned", "Scanning")
            scanner.process(mediaImage)
                .addOnSuccessListener { barcodes ->
                    Log.e("Scanned", "Success")

                    for (barcode in barcodes) {
                        Log.e("Scanned", barcode.rawValue.toString())
                        luma(barcode.rawValue)
                    }
                    if (barcodes.isEmpty()) {
                        imageProxy.close()
                    }


                }.addOnFailureListener {
                    Log.e("Scanned", "Failure")
                    imageProxy.close()
                }

        } else {
            imageProxy.close()
        }

    }
}