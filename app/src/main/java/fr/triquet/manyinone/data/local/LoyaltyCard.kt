package fr.triquet.manyinone.data.local

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "loyalty_cards")
data class LoyaltyCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcodeValue: String,
    val barcodeFormat: String,
    val color: Int = DEFAULT_COLOR,
    val createdAt: Long = System.currentTimeMillis(),
) {
    @get:Ignore
    val backgroundColor: Color
        get() = Color(color.toLong() or 0xFF000000L)

    @get:Ignore
    val contrastTextColor: Color
        get() {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            return if (luminance > 0.5) Color.Black else Color.White
        }

    companion object {
        const val DEFAULT_COLOR = 0xFF6750A4.toInt()
    }
}
