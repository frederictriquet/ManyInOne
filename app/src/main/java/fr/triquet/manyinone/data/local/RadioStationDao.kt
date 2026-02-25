package fr.triquet.manyinone.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import fr.triquet.manyinone.radio.RadioStation
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioStationDao {
    @Query("SELECT * FROM radio_stations ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<RadioStation>>

    @Query("SELECT * FROM radio_stations WHERE id = :id")
    suspend fun getById(id: Long): RadioStation?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM radio_stations")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(station: RadioStation): Long

    @Update
    suspend fun update(station: RadioStation)

    @Delete
    suspend fun delete(station: RadioStation)

    @Query("SELECT COUNT(*) FROM radio_stations")
    suspend fun count(): Int

    @Query("UPDATE radio_stations SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
