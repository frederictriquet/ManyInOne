package fr.triquet.manyinone.scanner

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (List<Barcode>) -> Unit,
    // Scanning runs on Play Services, so it can fail when the barcode module has not
    // been downloaded yet. Reported instead of leaving a camera that detects nothing.
    private val onError: (Throwable) -> Unit = {},
) : ImageAnalysis.Analyzer {

    private var errorReported = false

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onBarcodeDetected(barcodes)
                }
            }
            .addOnFailureListener { error ->
                Log.e("BarcodeAnalyzer", "Barcode scanning failed", error)
                // Every frame fails the same way; report once, not sixty times a second.
                if (!errorReported) {
                    errorReported = true
                    onError(error)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
