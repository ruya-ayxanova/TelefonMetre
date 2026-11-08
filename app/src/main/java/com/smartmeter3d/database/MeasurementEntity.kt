package com.smartmeter3d.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartmeter3d.calculator.LengthUnit

/** Tüm uzunluk/alan/hacim değerleri metre, m², m³ olarak saklanır. [unit] yalnızca gösterim birimidir. */
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectName: String,
    val type: MeasurementType,
    val createdAt: Long = System.currentTimeMillis(),
    val length: Double? = null,
    val width: Double? = null,
    val height: Double? = null,
    val depth: Double? = null,
    val area: Double? = null,
    val volume: Double? = null,
    val unit: LengthUnit = LengthUnit.METER,
    val method: MeasurementMethod,
    val photoPath: String? = null
)
