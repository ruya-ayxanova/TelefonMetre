package com.smartmeter3d.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.MeasurementError
import com.smartmeter3d.database.AppDatabase
import com.smartmeter3d.database.MeasurementEntity
import com.smartmeter3d.database.MeasurementType
import com.smartmeter3d.repository.MeasurementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = MeasurementRepository(AppDatabase.getInstance(app).measurementDao())

    private val filterState = MutableStateFlow<MeasurementType?>(null)
    val filter: StateFlow<MeasurementType?> = filterState.asStateFlow()

    /** Yeniden eskiye sıralı geçmiş. */
    val items: StateFlow<List<MeasurementEntity>> = filterState
        .flatMapLatest { f -> if (f == null) repository.observeAll() else repository.observeByType(f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var error by mutableStateOf<MeasurementError?>(null)
        private set

    fun selectFilter(type: MeasurementType?) {
        filterState.value = type
    }

    fun clearError() {
        error = null
    }

    /** Yalnızca ad ve tür düzenlenir; ölçü değerlerine dokunulmaz. */
    fun update(entity: MeasurementEntity, name: String, type: MeasurementType, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.update(entity.copy(projectName = name, type = type))
                error = null
                onSuccess()
            } catch (e: InvalidMeasurementException) {
                error = e.error
            }
        }
    }

    fun delete(entity: MeasurementEntity) {
        viewModelScope.launch {
            repository.delete(entity)
            val path = entity.photoPath
            if (path != null) withContext(Dispatchers.IO) { File(path).delete() }
        }
    }
}
