package com.smartmeter3d.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(entity: MeasurementEntity): Long

    @Update
    suspend fun update(entity: MeasurementEntity): Int

    @Delete
    suspend fun delete(entity: MeasurementEntity): Int

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getById(id: Long): MeasurementEntity?

    @Query("SELECT * FROM measurements ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE type = :type ORDER BY createdAt DESC, id DESC")
    fun observeByType(type: MeasurementType): Flow<List<MeasurementEntity>>
}
