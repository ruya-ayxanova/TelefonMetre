package com.smartmeter3d.repository

import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.MeasurementCalculator
import com.smartmeter3d.calculator.MeasurementError
import com.smartmeter3d.database.MeasurementDao
import com.smartmeter3d.database.MeasurementEntity
import com.smartmeter3d.database.MeasurementType
import kotlinx.coroutines.flow.Flow

class MeasurementRepository(private val dao: MeasurementDao) {

    fun observeAll(): Flow<List<MeasurementEntity>> = dao.observeAll()

    fun observeByType(type: MeasurementType): Flow<List<MeasurementEntity>> = dao.observeByType(type)

    suspend fun getById(id: Long): MeasurementEntity? = dao.getById(id)

    /** Geçersizse InvalidMeasurementException fırlatır; hiçbir şey kaydedilmez. */
    suspend fun save(entity: MeasurementEntity): Long {
        val clean = validated(entity)
        return dao.insert(clean)
    }

    suspend fun update(entity: MeasurementEntity) {
        val clean = validated(entity)
        dao.update(clean)
    }

    suspend fun delete(entity: MeasurementEntity) {
        dao.delete(entity)
    }

    private fun validated(e: MeasurementEntity): MeasurementEntity {
        val name = e.projectName.trim()
        if (name.isEmpty()) throw InvalidMeasurementException(MeasurementError.EMPTY_NAME)

        val dimensions = listOf(e.length, e.width, e.height, e.depth)
        val derived = listOf(e.area, e.volume)
        if ((dimensions + derived).all { it == null }) {
            throw InvalidMeasurementException(MeasurementError.EMPTY_INPUT)
        }
        dimensions.forEach { v -> if (v != null) MeasurementCalculator.validateDimension(v) }
        derived.forEach { v ->
            if (v != null) {
                if (v.isNaN()) throw InvalidMeasurementException(MeasurementError.NOT_A_NUMBER)
                if (v.isInfinite()) throw InvalidMeasurementException(MeasurementError.NOT_FINITE)
                if (v < 0.0) throw InvalidMeasurementException(MeasurementError.NEGATIVE_VALUE)
            }
        }
        return e.copy(projectName = name)
    }
}
