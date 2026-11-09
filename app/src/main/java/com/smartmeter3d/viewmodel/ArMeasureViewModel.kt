package com.smartmeter3d.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter3d.ar.ArFrameState
import com.smartmeter3d.calculator.InputParser
import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.LengthUnit
import com.smartmeter3d.calculator.MeasurementError
import com.smartmeter3d.database.AppDatabase
import com.smartmeter3d.database.MeasurementEntity
import com.smartmeter3d.database.MeasurementMethod
import com.smartmeter3d.database.MeasurementType
import com.smartmeter3d.measurement.ArComparison
import com.smartmeter3d.measurement.ArMath
import com.smartmeter3d.measurement.ArMode
import com.smartmeter3d.measurement.ArOutcome
import com.smartmeter3d.repository.MeasurementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArMeasureViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = MeasurementRepository(AppDatabase.getInstance(app).measurementDao())

    private val frameState = MutableStateFlow(ArFrameState())
    val frame: StateFlow<ArFrameState> = frameState.asStateFlow()

    var mode by mutableStateOf(ArMode.DISTANCE)
        private set
    var error by mutableStateOf<MeasurementError?>(null)
        private set
    var saved by mutableStateOf(false)
        private set
    var comparison by mutableStateOf<ArComparison?>(null)
        private set

    private var newChainRequested = false

    /** GL iş parçacığından çağrılır. */
    fun onFrame(state: ArFrameState) {
        frameState.value = state
    }

    fun selectMode(newMode: ArMode) {
        if (newMode == mode) return
        mode = newMode
        newChainRequested = false
        resetResultState()
    }

    fun requestNewChain() {
        newChainRequested = true
    }

    /** Bir sonraki nokta yeni kenar zincirini başlatıyor mu? (tek kullanımlık) */
    fun consumeNewChainFlag(): Boolean {
        val value = newChainRequested
        newChainRequested = false
        return value
    }

    fun resetResultState() {
        comparison = null
        saved = false
        error = null
    }

    fun compare(arMeters: Double, text: String, unit: LengthUnit): Boolean =
        try {
            comparison = ArMath.compare(arMeters, InputParser.parseLengthToMeters(text, unit))
            error = null
            true
        } catch (e: InvalidMeasurementException) {
            error = e.error
            false
        }

    fun dismissError() {
        error = null
    }

    fun save(name: String, type: MeasurementType, outcome: ArOutcome) {
        val entity = when (outcome) {
            is ArOutcome.Distance -> MeasurementEntity(
                projectName = name, type = type, length = outcome.total,
                unit = LengthUnit.CENTIMETER, method = MeasurementMethod.AR
            )
            is ArOutcome.Rect -> MeasurementEntity(
                projectName = name, type = type,
                width = outcome.result.width, height = outcome.result.height, area = outcome.result.area,
                unit = LengthUnit.CENTIMETER, method = MeasurementMethod.AR
            )
        }
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
