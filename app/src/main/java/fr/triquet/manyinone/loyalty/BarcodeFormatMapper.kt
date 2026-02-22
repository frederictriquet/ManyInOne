package fr.triquet.manyinone.loyalty

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

    val displayNames: List<String> = mapping.keys.toList()

    fun toZxing(displayName: String): BarcodeFormat? = mapping[displayName]

    fun is2D(displayName: String): Boolean {
        return displayName in setOf("QR Code", "PDF417", "Aztec", "Data Matrix")
    }
}
