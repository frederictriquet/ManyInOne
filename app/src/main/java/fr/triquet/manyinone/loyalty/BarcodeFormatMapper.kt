package fr.triquet.manyinone.loyalty

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat

object BarcodeFormatMapper {
    private val mapping = mapOf(
        "QR Code" to BarcodeFormat.QR_CODE,
        "EAN-13" to BarcodeFormat.EAN_13,
        "EAN-8" to BarcodeFormat.EAN_8,
        "UPC-A" to BarcodeFormat.UPC_A,
        "UPC-E" to BarcodeFormat.UPC_E,
        "Code 128" to BarcodeFormat.CODE_128,
        "Code 39" to BarcodeFormat.CODE_39,
        "Code 93" to BarcodeFormat.CODE_93,
        "ITF" to BarcodeFormat.ITF,
        "PDF417" to BarcodeFormat.PDF_417,
        "Aztec" to BarcodeFormat.AZTEC,
        "Data Matrix" to BarcodeFormat.DATA_MATRIX,
        "Codabar" to BarcodeFormat.CODABAR,
    )

    private val mlKitMapping = mapOf(
        Barcode.FORMAT_QR_CODE to "QR Code",
        Barcode.FORMAT_EAN_13 to "EAN-13",
        Barcode.FORMAT_EAN_8 to "EAN-8",
        Barcode.FORMAT_UPC_A to "UPC-A",
        Barcode.FORMAT_UPC_E to "UPC-E",
        Barcode.FORMAT_CODE_128 to "Code 128",
        Barcode.FORMAT_CODE_39 to "Code 39",
        Barcode.FORMAT_CODE_93 to "Code 93",
        Barcode.FORMAT_ITF to "ITF",
        Barcode.FORMAT_PDF417 to "PDF417",
        Barcode.FORMAT_AZTEC to "Aztec",
        Barcode.FORMAT_DATA_MATRIX to "Data Matrix",
        Barcode.FORMAT_CODABAR to "Codabar",
    )

    val displayNames: List<String> = mapping.keys.toList()

    fun toZxing(displayName: String): BarcodeFormat? = mapping[displayName]

    fun fromMlKit(format: Int): String = mlKitMapping[format] ?: "Barcode"

    fun is2D(displayName: String): Boolean {
        return displayName in setOf("QR Code", "PDF417", "Aztec", "Data Matrix")
    }
}
