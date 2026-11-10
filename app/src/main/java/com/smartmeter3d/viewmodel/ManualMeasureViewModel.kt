package com.smartmeter3d.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.LengthUnit
import com.smartmeter3d.calculator.ManualResult
import com.smartmeter3d.calculator.MeasurementError
import com.smartmeter3d.database.AppDatabase
import com.smartmeter3d.database.MeasurementEntity
import com.smartmeter3d.database.MeasurementMethod
import com.smartmeter3d.database.MeasurementType
import com.smartmeter3d.repository.MeasurementRepository
import kotlinx.coroutines.launch

class ManualMeasureViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = MeasurementRepository(AppDatabase.getInstance(app).measurementDao())

    var saved by mutableStateOf(false)
        private set
    var error by mutableStateOf<MeasurementError?>(null)
        private set

    fun clearStatus() {
        saved = false
        error = null
    }

    fun save(name: String, type: MeasurementType, result: ManualResult, unit: LengthUnit) {
        val entity = MeasurementEntity(
            projectName = name,
            type = type,
            length = result.length,
            width = result.width,
            height = result.height,
            depth = result.depth,
            area = result.area,
            volume = result.volume,
            unit = unit,
            method = MeasurementMethod.MANUAL
        )
        viewModelScope.launch {
            try {
                repository.save(entity)
                saved = true
                error = null
            } catch (e: InvalidMeasurementException) {
                error = e.error
            }
        }
    }
}
