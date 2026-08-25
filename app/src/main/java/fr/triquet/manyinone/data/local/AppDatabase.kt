package fr.triquet.manyinone.data.local

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.triquet.manyinone.radio.DEFAULT_STATIONS
import fr.triquet.manyinone.radio.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [LoyaltyCard::class, RadioStation::class],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loyaltyCardDao(): LoyaltyCardDao
    abstract fun radioStationDao(): RadioStationDao

    companion object {
        private const val DB_NAME = "manyinone.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /** Closes the singleton so the next getInstance reopens the database. */
        @VisibleForTesting
        fun closeForTests() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        /** Closes the singleton and wipes the database file. */
        @VisibleForTesting
        fun resetForTests(context: Context) {
            closeForTests()
            context.applicationContext.deleteDatabase(DB_NAME)
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            return try {
                createDatabase(appContext).also { db ->
                    // Force-open to detect migration issues now rather than on first query
                    db.openHelper.writableDatabase
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Migration failed, recreating database", e)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        appContext,
                        "Mise à jour de l'app : les données locales ont été réinitialisées.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                appContext.deleteDatabase(DB_NAME)
                INSTANCE = null
                createDatabase(appContext)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE radio_stations ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loyalty_cards ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE radio_stations SET sortOrder = (SELECT COUNT(*) FROM radio_stations AS r2 WHERE r2.rowid < radio_stations.rowid)")
                db.execSQL("UPDATE loyalty_cards SET sortOrder = (SELECT COUNT(*) FROM loyalty_cards AS c2 WHERE c2.rowid < loyalty_cards.rowid)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """INSERT INTO radio_stations (name, streamUrl, description, createdAt, sortOrder)
                       VALUES ('Ibiza Global Radio',
                               'https://listenssl.ibizaglobalradio.com:8024/;',
                               'The soundtrack of Ibiza — Dance & House — MP3 128kbps',
                               ${System.currentTimeMillis()},
                               (SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM radio_stations))"""
                )
            }
        }

        /** Fills an empty station list with [DEFAULT_STATIONS]. No-op otherwise. */
        private fun seedDefaultStations(db: SupportSQLiteDatabase) {
            val count = db.query("SELECT COUNT(*) FROM radio_stations").use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            if (count > 0) return

            DEFAULT_STATIONS.forEach { station ->
                db.execSQL(
                    """INSERT INTO radio_stations (name, streamUrl, description, createdAt, sortOrder)
                       VALUES (?, ?, ?, ?, ?)""",
                    arrayOf(
                        station.name,
                        station.streamUrl,
                        station.description,
                        station.createdAt,
                        station.sortOrder,
                    ),
                )
            }
            Log.i("AppDatabase", "Inserted ${DEFAULT_STATIONS.size} default radio stations")
        }

        private fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Writes through `db` rather than the DAO: this runs while the
                        // database is being opened, before INSTANCE is assigned.
                        try {
                            seedDefaultStations(db)
                        } catch (e: Exception) {
                            // Swallowed on purpose: letting it escape would trip the
                            // catch in buildDatabase, which deletes the user's data.
                            Log.e("AppDatabase", "Failed to insert the default radio stations", e)
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(
                                    context,
                                    "Impossible d'ajouter les radios par défaut : ${e.message}",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    }
                })
                .build()
        }
    }
}
