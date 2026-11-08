package com.smartmeter3d.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.MeasurementError
import com.smartmeter3d.database.AppDatabase
import com.smartmeter3d.database.MeasurementEntity
import com.smartmeter3d.database.MeasurementMethod
import com.smartmeter3d.database.MeasurementType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeasurementRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: MeasurementRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = MeasurementRepository(db.measurementDao())
    }

    @After fun tearDown() { db.close() }

    private fun valid() = MeasurementEntity(
        projectName = "  Salon  ", type = MeasurementType.ROOM,
        length = 4.0, width = 3.0, height = 2.5, area = 12.0, volume = 30.0,
        method = MeasurementMethod.MANUAL
    )

    private suspend fun errorOf(entity: MeasurementEntity): MeasurementError =
        try { repo.save(entity); error("hata bekleniyordu") }
        catch (e: InvalidMeasurementException) { e.error }

    @Test fun save_trimsNameAndPersists() = runTest {
        val id = repo.save(valid())
        assertEquals("Salon", repo.getById(id)!!.projectName)
    }

    @Test fun save_rejectsBlankName() = runTest {
        assertEquals(MeasurementError.EMPTY_NAME, errorOf(valid().copy(projectName = "   ")))
        assertEquals(0, repo.observeAll().first().size)
    }

    @Test fun save_rejectsNegativeZeroAndNaNDimensions() = runTest {
        assertEquals(MeasurementError.NEGATIVE_VALUE, errorOf(valid().copy(length = -1.0)))
        assertEquals(MeasurementError.ZERO_VALUE, errorOf(valid().copy(width = 0.0)))
        assertEquals(MeasurementError.NOT_A_NUMBER, errorOf(valid().copy(height = Double.NaN)))
        assertEquals(MeasurementError.NOT_FINITE, errorOf(valid().copy(area = Double.POSITIVE_INFINITY)))
        assertEquals(0, repo.observeAll().first().size)
    }

    @Test fun save_rejectsEntityWithNoValues() = runTest {
        val empty = valid().copy(length = null, width = null, height = null, depth = null, area = null, volume = null)
        assertEquals(MeasurementError.EMPTY_INPUT, errorOf(empty))
    }

    @Test fun update_validatesAndStores() = runTest {
        val id = repo.save(valid())
        val stored = repo.getById(id)!!
        repo.update(stored.copy(projectName = "Mutfak"))
        assertEquals("Mutfak", repo.getById(id)!!.projectName)
        assertThrows(InvalidMeasurementException::class.java) {
            kotlinx.coroutines.runBlocking { repo.update(stored.copy(length = -2.0)) }
        }
        assertEquals("Mutfak", repo.getById(id)!!.projectName)
    }

    @Test fun delete_removesFromHistory() = runTest {
        val id = repo.save(valid())
        repo.delete(repo.getById(id)!!)
        assertEquals(0, repo.observeAll().first().size)
    }
}
