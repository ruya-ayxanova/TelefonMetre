package com.smartmeter3d.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter3d.calculator.InputParser
import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.LengthUnit
import com.smartmeter3d.calculator.MeasurementError
import com.smartmeter3d.camera.PhotoStorage
import com.smartmeter3d.database.AppDatabase
import com.smartmeter3d.database.MeasurementEntity
import com.smartmeter3d.database.MeasurementMethod
import com.smartmeter3d.database.MeasurementType
import com.smartmeter3d.measurement.ImageAnnotator
import com.smartmeter3d.measurement.PhotoMath
import com.smartmeter3d.measurement.Point2
import com.smartmeter3d.measurement.RectangleMeasurer
import com.smartmeter3d.measurement.RectangleResult
import com.smartmeter3d.repository.MeasurementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

enum class PhotoMode(val refCount: Int, val targetCount: Int) {
    DISTANCE(2, 2),
    RECTANGLE(4, 4)
}

enum class PhotoStep { NO_IMAGE, REF_POINTS, REF_INPUT, TARGET_POINTS, DONE }

sealed interface ReferenceInput {
    data class Length(val meters: Double) : ReferenceInput
    data class Rect(val width: Double, val height: Double) : ReferenceInput
}

sealed interface PhotoOutcome {
    data class Distance(val meters: Double) : PhotoOutcome
    data class Rectangle(val result: RectangleResult) : PhotoOutcome
}

class PhotoMeasureViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = PhotoStorage(app)
    private val repository = MeasurementRepository(AppDatabase.getInstance(app).measurementDao())
    private var pendingCapture: File? = null

    var mode by mutableStateOf(PhotoMode.DISTANCE)
        private set
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var cameraFailed by mutableStateOf(false)
        private set
    var reference by mutableStateOf<ReferenceInput?>(null)
        private set
    var outcome by mutableStateOf<PhotoOutcome?>(null)
        private set
    var error by mutableStateOf<MeasurementError?>(null)
        private set
    var saved by mutableStateOf(false)
        private set
    var saveFailed by mutableStateOf(false)
        private set
    var refUnit by mutableStateOf(LengthUnit.CENTIMETER)
        private set

    val refPoints = mutableStateListOf<Point2>()
    val targetPoints = mutableStateListOf<Point2>()

    val step: PhotoStep
        get() = when {
            bitmap == null -> PhotoStep.NO_IMAGE
            refPoints.size < mode.refCount -> PhotoStep.REF_POINTS
            reference == null -> PhotoStep.REF_INPUT
            targetPoints.size < mode.targetCount -> PhotoStep.TARGET_POINTS
            else -> PhotoStep.DONE
        }

    // ---------- fotoğraf kaynağı ----------
    fun prepareCameraUri(): Uri? =
        try {
            val file = storage.newCaptureFile()
            pendingCapture = file
            cameraFailed = false
            storage.uriFor(file)
        } catch (e: Exception) {
            cameraFailed = true
            null
        }

    fun onCameraResult(success: Boolean) {
        val file = pendingCapture ?: return
        pendingCapture = null
        if (!success) {
            file.delete()
            return
        }
        viewModelScope.launch { decodeAndShow(file) }
    }

    fun onImagePicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { storage.importFromUri(uri) }
            if (file == null) loadFailed = true else decodeAndShow(file)
        }
    }

    private suspend fun decodeAndShow(file: File) {
        val bmp = withContext(Dispatchers.IO) { storage.decode(file) }
        file.delete()
        if (bmp == null) {
            loadFailed = true
        } else {
            loadFailed = false
            clearAll()
            bitmap = bmp
        }
    }

    // ---------- nokta işlemleri ----------
    fun selectMode(newMode: PhotoMode) {
        if (newMode == mode) return
        mode = newMode
        clearAll()
    }

    fun addPoint(p: Point2) {
        val bmp = bitmap ?: return
        val c = clamp(p, bmp)
        when (step) {
            PhotoStep.REF_POINTS -> refPoints.add(c)
            PhotoStep.TARGET_POINTS -> targetPoints.add(c)
            else -> return
        }
        saved = false
        error = null
        recompute()
    }

    /** [index], referans noktaları + hedef noktaları birleşik listesindeki sıradır. */
    fun movePoint(index: Int, p: Point2) {
        val bmp = bitmap ?: return
        val c = clamp(p, bmp)
        if (index in refPoints.indices) {
            refPoints[index] = c
        } else {
            val t = index - refPoints.size
            if (t in targetPoints.indices) targetPoints[t] = c else return
        }
        saved = false
        recompute()
    }

    fun undo() {
        error = null
        saved = false
        if (targetPoints.isNotEmpty()) {
            targetPoints.removeAt(targetPoints.lastIndex)
        } else if (refPoints.isNotEmpty()) {
            refPoints.removeAt(refPoints.lastIndex)
            reference = null
        }
        recompute()
    }

    fun newMeasure() {
        targetPoints.clear()
        error = null
        saved = false
        recompute()
    }

    fun clearAll() {
        refPoints.clear()
        targetPoints.clear()
        reference = null
        outcome = null
        error = null
        saved = false
        saveFailed = false
    }

    // ---------- referans ----------
    fun confirmReference(first: String, second: String, unit: LengthUnit) {
        try {
            reference = when (mode) {
                PhotoMode.DISTANCE -> ReferenceInput.Length(InputParser.parseLengthToMeters(first, unit))
                PhotoMode.RECTANGLE -> ReferenceInput.Rect(
                    InputParser.parseLengthToMeters(first, unit),
                    InputParser.parseLengthToMeters(second, unit)
                )
            }
            refUnit = unit
            error = null
            recompute()
        } catch (e: InvalidMeasurementException) {
            error = e.error
        }
    }

    fun cancelReference() {
        refPoints.clear()
        reference = null
        error = null
    }

    // ---------- hesaplama ----------
    private fun recompute() {
        outcome = null
        if (step != PhotoStep.DONE) return
        val ref = reference
        try {
            outcome = when {
                mode == PhotoMode.DISTANCE && ref is ReferenceInput.Length ->
                    PhotoOutcome.Distance(
                        PhotoMath.measureDistance(refPoints[0], refPoints[1], ref.meters, targetPoints[0], targetPoints[1])
                    )
                mode == PhotoMode.RECTANGLE && ref is ReferenceInput.Rect ->
                    PhotoOutcome.Rectangle(
                        RectangleMeasurer.measure(refPoints.toList(), ref.width, ref.height, targetPoints.toList())
                    )
                else -> null
            }
            error = null
        } catch (e: InvalidMeasurementException) {
            error = e.error
        }
    }

    // ---------- kaydetme ----------
    fun save(name: String, type: MeasurementType) {
        val bmp = bitmap ?: return
        val result = outcome ?: return
        val ref = reference ?: return
        val refList = refPoints.toList()
        val targetList = targetPoints.toList()
        val closeShapes = mode == PhotoMode.RECTANGLE
        val unit = refUnit
        val caption = captionFor(result, ref)

        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) {
                    val annotated = ImageAnnotator.render(bmp, refList, targetList, closeShapes, caption)
                    try {
                        storage.saveAnnotated(annotated).absolutePath
                    } finally {
                        annotated.recycle()
                    }
                }
                val entity = when (result) {
                    is PhotoOutcome.Distance -> MeasurementEntity(
                        projectName = name, type = type, length = result.meters,
                        unit = unit, method = MeasurementMethod.PHOTO, photoPath = path
                    )
                    is PhotoOutcome.Rectangle -> MeasurementEntity(
                        projectName = name, type = type,
                        width = result.result.width, height = result.result.height, area = result.result.area,
                        unit = unit, method = MeasurementMethod.PHOTO, photoPath = path
                    )
                }
                repository.save(entity)
                saved = true
                saveFailed = false
                error = null
            } catch (e: InvalidMeasurementException) {
                error = e.error
            } catch (e: IOException) {
                saveFailed = true
            }
        }
    }

    private fun captionFor(result: PhotoOutcome, ref: ReferenceInput): List<String> {
        fun f(v: Double, d: Int) = String.format(Locale.US, "%.${d}f", v)
        return when (result) {
            is PhotoOutcome.Distance -> listOf(
                "L = ${f(result.meters, 3)} m (${f(result.meters * 100, 1)} cm)",
                "ref = ${(ref as? ReferenceInput.Length)?.let { f(it.meters * 100, 1) } ?: "?"} cm"
            )
            is PhotoOutcome.Rectangle -> {
                val r = result.result
                listOf(
                    "W = ${f(r.width, 3)} m   H = ${f(r.height, 3)} m",
                    "A = ${f(r.area, 3)} m2 (${f(r.area * 10000, 0)} cm2)",
                    "Smart Meter 3D"
                )
            }
        }
    }

    private fun clamp(p: Point2, bmp: Bitmap) = Point2(
        p.x.coerceIn(0.0, (bmp.width - 1).toDouble()),
        p.y.coerceIn(0.0, (bmp.height - 1).toDouble())
    )
}
