package fr.triquet.manyinone.data.local

import androidx.room.Entity
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
    companion object {
        const val DEFAULT_COLOR = 0xFF6750A4.toInt() // Material Purple
    }
}
