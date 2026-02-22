package fr.triquet.manyinone.loyalty

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.MultiFormatWriter

private const val TAG = "BarcodeImage"

@Composable
fun BarcodeImage(
    value: String,
    format: String,
    width: Int,
    height: Int,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(value, format, width, height) {
        generateBarcodeBitmap(value, format, width, height)
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Barcode",
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

private fun generateBarcodeBitmap(
    value: String,
    format: String,
    width: Int,
    height: Int,
): Bitmap? {
    val zxingFormat = BarcodeFormatMapper.toZxing(format) ?: return null
    return try {
        val bitMatrix = MultiFormatWriter().encode(value, zxingFormat, width, height)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        bmp
    } catch (e: Exception) {
        Log.w(TAG, "Failed to generate barcode: format=$format, value=$value", e)
        null
    }
}
