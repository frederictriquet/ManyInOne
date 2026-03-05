package fr.triquet.manyinone.loyalty

import com.google.zxing.BarcodeFormat
import org.junit.Assert.*
import org.junit.Test

class BarcodeFormatMapperTest {

    @Test
    fun `displayNames contains all 13 expected formats`() {
        val expected = setOf(
            "QR Code", "EAN-13", "EAN-8", "UPC-A", "UPC-E",
            "Code 128", "Code 39", "Code 93", "ITF", "PDF417",
            "Aztec", "Data Matrix", "Codabar",
        )
        assertEquals(expected, BarcodeFormatMapper.displayNames.toSet())
    }

    @Test
    fun `toZxing maps QR Code correctly`() {
        assertEquals(BarcodeFormat.QR_CODE, BarcodeFormatMapper.toZxing("QR Code"))
    }

    @Test
    fun `toZxing maps EAN-13 correctly`() {
        assertEquals(BarcodeFormat.EAN_13, BarcodeFormatMapper.toZxing("EAN-13"))
    }

    @Test
    fun `toZxing maps Code 128 correctly`() {
        assertEquals(BarcodeFormat.CODE_128, BarcodeFormatMapper.toZxing("Code 128"))
    }

    @Test
    fun `toZxing returns null for unknown format`() {
        assertNull(BarcodeFormatMapper.toZxing("Unknown"))
    }

    @Test
    fun `toZxing returns null for empty string`() {
        assertNull(BarcodeFormatMapper.toZxing(""))
    }

    @Test
    fun `all displayNames resolve to a ZXing format`() {
        BarcodeFormatMapper.displayNames.forEach { name ->
            assertNotNull("$name devrait avoir un mapping ZXing", BarcodeFormatMapper.toZxing(name))
        }
    }

    @Test
    fun `is2D returns true for 2D formats`() {
        assertTrue(BarcodeFormatMapper.is2D("QR Code"))
        assertTrue(BarcodeFormatMapper.is2D("PDF417"))
        assertTrue(BarcodeFormatMapper.is2D("Aztec"))
        assertTrue(BarcodeFormatMapper.is2D("Data Matrix"))
    }

    @Test
    fun `is2D returns false for 1D formats`() {
        assertFalse(BarcodeFormatMapper.is2D("EAN-13"))
        assertFalse(BarcodeFormatMapper.is2D("EAN-8"))
        assertFalse(BarcodeFormatMapper.is2D("Code 128"))
        assertFalse(BarcodeFormatMapper.is2D("Code 39"))
        assertFalse(BarcodeFormatMapper.is2D("Code 93"))
        assertFalse(BarcodeFormatMapper.is2D("ITF"))
        assertFalse(BarcodeFormatMapper.is2D("Codabar"))
        assertFalse(BarcodeFormatMapper.is2D("UPC-A"))
        assertFalse(BarcodeFormatMapper.is2D("UPC-E"))
    }

    @Test
    fun `is2D returns false for unknown format`() {
        assertFalse(BarcodeFormatMapper.is2D("Unknown"))
    }

    @Test
    fun `fromMlKit returns fallback for unknown format code`() {
        assertEquals("Barcode", BarcodeFormatMapper.fromMlKit(-999))
    }
}
