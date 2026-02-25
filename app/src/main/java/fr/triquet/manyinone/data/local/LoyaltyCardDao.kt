package fr.triquet.manyinone.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {
    @Query("SELECT * FROM loyalty_cards ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<LoyaltyCard>>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id")
    suspend fun getById(id: Long): LoyaltyCard?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM loyalty_cards")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(card: LoyaltyCard): Long

    @Update
    suspend fun update(card: LoyaltyCard)

    @Delete
    suspend fun delete(card: LoyaltyCard)

    @Query("UPDATE loyalty_cards SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
