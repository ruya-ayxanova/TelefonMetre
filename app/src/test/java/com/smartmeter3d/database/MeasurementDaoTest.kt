package com.smartmeter3d.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.smartmeter3d.calculator.LengthUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeasurementDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: MeasurementDao

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.measurementDao()
    }

    @After fun tearDown() { db.close() }

    private fun sample(name: String, time: Long, type: MeasurementType = MeasurementType.WALL) =
        MeasurementEntity(
            projectName = name, type = type, createdAt = time,
            length = 5.0, height = 2.5, area = 12.5,
            unit = LengthUnit.CENTIMETER, method = MeasurementMethod.AR,
            photoPath = "/data/photo.jpg"
        )

    @Test fun insertThenRead_returnsSameValues() = runTest {
        val id = dao.insert(sample("Salon", 100L))
        val read = dao.getById(id)
        assertNotNull(read)
        assertEquals("Salon", read!!.projectName)
        assertEquals(MeasurementType.WALL, read.type)
        assertEquals(5.0, read.length!!, 1e-9)
        assertEquals(12.5, read.area!!, 1e-9)
        assertNull(read.width)
        assertEquals(LengthUnit.CENTIMETER, read.unit)
        assertEquals(MeasurementMethod.AR, read.method)
        assertEquals("/data/photo.jpg", read.photoPath)
        assertEquals(100L, read.createdAt)
    }

    @Test fun observeAll_isNewestFirst() = runTest {
        dao.insert(sample("eski", 100L))
        dao.insert(sample("yeni", 300L))
        dao.insert(sample("orta", 200L))
        assertEquals(listOf("yeni", "orta", "eski"), dao.observeAll().first().map { it.projectName })
    }

    @Test fun observeByType_filters() = runTest {
        dao.insert(sample("duvar", 1L, MeasurementType.WALL))
        dao.insert(sample("kapı", 2L, MeasurementType.DOOR))
        assertEquals(listOf("kapı"), dao.observeByType(MeasurementType.DOOR).first().map { it.projectName })
    }

    @Test fun update_changesStoredRow() = runTest {
        val id = dao.insert(sample("Salon", 100L))
        val stored = dao.getById(id)!!
        dao.update(stored.copy(projectName = "Yatak odası", height = 2.7))
        val read = dao.getById(id)!!
        assertEquals("Yatak odası", read.projectName)
        assertEquals(2.7, read.height!!, 1e-9)
    }

    @Test fun delete_removesRow() = runTest {
        val id = dao.insert(sample("Salon", 100L))
        dao.delete(dao.getById(id)!!)
        assertNull(dao.getById(id))
        assertEquals(0, dao.observeAll().first().size)
    }
}
