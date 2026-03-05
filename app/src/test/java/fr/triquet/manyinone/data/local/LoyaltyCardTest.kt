package fr.triquet.manyinone.data.local

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class LoyaltyCardTest {

    private fun card(color: Int) = LoyaltyCard(
        name = "Test",
        barcodeValue = "123456",
        barcodeFormat = "EAN-13",
        color = color,
    )

    @Test
    fun `white background gives black text`() {
        assertEquals(Color.Black, card(0xFFFFFF).contrastTextColor)
    }

    @Test
    fun `black background gives white text`() {
        assertEquals(Color.White, card(0x000000).contrastTextColor)
    }

    @Test
    fun `red background gives white text`() {
        // luminance ≈ 0.299 * 255 / 255 = 0.299 → < 0.5 → white
        assertEquals(Color.White, card(0xFF0000).contrastTextColor)
    }

    @Test
    fun `light yellow background gives black text`() {
        // luminance ≈ (0.299*255 + 0.587*255) / 255 = 0.886 → > 0.5 → black
        assertEquals(Color.Black, card(0xFFFF00).contrastTextColor)
    }

    @Test
    fun `backgroundColor applies full alpha`() {
        val bg = card(0xFF0000).backgroundColor
        assertEquals(Color(0xFFFF0000L), bg)
    }

    @Test
    fun `backgroundColor with existing alpha preserves full alpha`() {
        // Couleur avec alpha partiel → on force FF
        val bg = card(0x80FF0000.toInt()).backgroundColor
        // Le masque force alpha = FF
        assertEquals(Color(0xFFFF0000L), bg)
    }

    @Test
    fun `DEFAULT_COLOR is the Material purple`() {
        assertEquals(0xFF6750A4.toInt(), LoyaltyCard.DEFAULT_COLOR)
    }

    @Test
    fun `default color produces a card without error`() {
        val c = LoyaltyCard(name = "Test", barcodeValue = "X", barcodeFormat = "QR Code")
        assertEquals(LoyaltyCard.DEFAULT_COLOR, c.color)
        assertNotNull(c.contrastTextColor)
    }

    @Test
    fun `sortOrder defaults to 0`() {
        val c = LoyaltyCard(name = "Test", barcodeValue = "X", barcodeFormat = "QR Code")
        assertEquals(0, c.sortOrder)
    }
}
