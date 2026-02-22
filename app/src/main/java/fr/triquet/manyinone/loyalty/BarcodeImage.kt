package fr.triquet.manyinone.loyalty

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

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
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(value, zxingFormat, width, height)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (_: Exception) {
        null
    }
}
