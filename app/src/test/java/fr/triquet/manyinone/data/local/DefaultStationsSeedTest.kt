package fr.triquet.manyinone.data.local

import androidx.test.core.app.ApplicationProvider
import fr.triquet.manyinone.radio.DEFAULT_STATIONS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the default radio stations, which used to be silently skipped: the seeding
 * callback read INSTANCE, still null while the database was being opened, so a fresh
 * install ended up with an empty station list.
 *
 * Goes through AppDatabase.getInstance rather than an in-memory builder, since the
 * bug lived in that very wiring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DefaultStationsSeedTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun clearDatabase() {
        AppDatabase.resetForTests(context)
    }

    @After
    fun tearDown() {
        AppDatabase.resetForTests(context)
    }

    @Test
    fun freshDatabase_containsTheDefaultStations() = runBlocking {
        val dao = AppDatabase.getInstance(context).radioStationDao()

        assertEquals(DEFAULT_STATIONS.size, dao.count())
    }

    @Test
    fun freshDatabase_keepsTheDefaultStationNamesAndUrls() = runBlocking {
        val dao = AppDatabase.getInstance(context).radioStationDao()
        val stored = dao.getAll().first()

        assertEquals(DEFAULT_STATIONS.map { it.name }, stored.map { it.name })
        assertEquals(DEFAULT_STATIONS.map { it.streamUrl }, stored.map { it.streamUrl })
    }

    @Test
    fun existingStations_areNotDuplicatedOnReopen() = runBlocking {
        AppDatabase.getInstance(context).radioStationDao().count()
        AppDatabase.closeForTests()

        // Reopening runs the onOpen callback again; it must not re-seed.
        val dao = AppDatabase.getInstance(context).radioStationDao()

        assertEquals(DEFAULT_STATIONS.size, dao.count())
    }
}
