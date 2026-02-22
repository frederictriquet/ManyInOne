package fr.triquet.manyinone.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {
    @Query("SELECT * FROM loyalty_cards ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LoyaltyCard>>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id")
    suspend fun getById(id: Long): LoyaltyCard?

    @Insert
    suspend fun insert(card: LoyaltyCard): Long

    @Update
    suspend fun update(card: LoyaltyCard)

    @Delete
    suspend fun delete(card: LoyaltyCard)
}
