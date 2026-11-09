package com.smartmeter3d.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter3d.R
import com.smartmeter3d.calculator.LengthUnit
import com.smartmeter3d.database.MeasurementType
import com.smartmeter3d.ui.components.PhotoCanvas
import com.smartmeter3d.ui.messageRes
import com.smartmeter3d.viewmodel.PhotoMeasureViewModel
import com.smartmeter3d.viewmodel.PhotoMode
import com.smartmeter3d.viewmodel.PhotoOutcome
import com.smartmeter3d.viewmodel.PhotoStep
import java.util.Locale

private fun fmt(v: Double, digits: Int): String = String.format(Locale.US, "%.${digits}f", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoMeasureScreen(onBack: () -> Unit, vm: PhotoMeasureViewModel = viewModel()) {
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        vm.onCameraResult(ok)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.onImagePicked(uri)
    }
    var showSave by remember { mutableStateOf(false) }
    val step = vm.step
    val bmp = vm.bitmap

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_photo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { vm.prepareCameraUri()?.let { takePicture.launch(it) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text(stringResource(R.string.photo_take), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Text(stringResource(R.string.photo_pick), modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.mode == PhotoMode.DISTANCE,
                    onClick = { vm.selectMode(PhotoMode.DISTANCE) },
                    label = { Text(stringResource(R.string.photo_mode_distance)) }
                )
                FilterChip(
                    selected = vm.mode == PhotoMode.RECTANGLE,
                    onClick = { vm.selectMode(PhotoMode.RECTANGLE) },
                    label = { Text(stringResource(R.string.photo_mode_rect)) }
                )
            }

            if (vm.loadFailed) ErrorText(stringResource(R.string.photo_load_failed))
            if (vm.cameraFailed) ErrorText(stringResource(R.string.photo_camera_failed))

            if (bmp == null) {
                Text(stringResource(R.string.photo_no_image), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(stringResource(stepTextRes(step, vm.mode)), style = MaterialTheme.typography.bodyLarge)

                PhotoCanvas(
                    bitmap = bmp,
                    refPoints = vm.refPoints,
                    targetPoints = vm.targetPoints,
                    closeShapes = vm.mode == PhotoMode.RECTANGLE,
                    onTap = vm::addPoint,
                    onMove = vm::movePoint
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::undo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Text(stringResource(R.string.photo_undo), modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = vm::clearAll) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text(stringResource(R.string.photo_clear), modifier = Modifier.padding(start = 6.dp))
                    }
                }

                vm.error?.let { ErrorText(stringResource(it.messageRes())) }

                vm.outcome?.let { outcome ->
                    ResultCard(outcome)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showSave = true }) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Text(stringResource(R.string.photo_save), modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = vm::newMeasure) {
                            Text(stringResource(R.string.photo_new_measure))
                        }
                    }
                }
                if (vm.saved) Text(stringResource(R.string.photo_saved), color = MaterialTheme.colorScheme.primary)
                if (vm.saveFailed) ErrorText(stringResource(R.string.photo_save_failed))
            }
        }
    }

    if (step == PhotoStep.REF_INPUT) {
        ReferenceDialog(
            mode = vm.mode,
            errorRes = vm.error?.messageRes(),
            onConfirm = vm::confirmReference,
            onCancel = vm::cancelReference
        )
    }
    if (showSave) {
        SaveDialog(
            onConfirm = { name, type ->
                showSave = false
                vm.save(name, type)
            },
            onCancel = { showSave = false }
        )
    }
}

private fun stepTextRes(step: PhotoStep, mode: PhotoMode): Int = when (step) {
    PhotoStep.NO_IMAGE -> R.string.photo_no_image
    PhotoStep.REF_POINTS ->
        if (mode == PhotoMode.DISTANCE) R.string.photo_step_ref_line else R.string.photo_step_ref_rect
    PhotoStep.REF_INPUT -> R.string.photo_step_ref_input
    PhotoStep.TARGET_POINTS ->
        if (mode == PhotoMode.DISTANCE) R.string.photo_step_measure_line else R.string.photo_step_target_rect
    PhotoStep.DONE -> R.string.photo_step_done
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun ResultCard(outcome: PhotoOutcome) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (outcome) {
                is PhotoOutcome.Distance -> {
                    val m = outcome.meters
                    Text(
                        "${stringResource(R.string.result_distance)}: ${fmt(m, 3)} m (${fmt(m * 100, 1)} cm)",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                is PhotoOutcome.Rectangle -> {
                    val r = outcome.result
                    Text("${stringResource(R.string.result_width)}: ${fmt(r.width, 3)} m (${fmt(r.width * 100, 1)} cm)", style = MaterialTheme.typography.titleMedium)
                    Text("${stringResource(R.string.result_height)}: ${fmt(r.height, 3)} m (${fmt(r.height * 100, 1)} cm)", style = MaterialTheme.typography.titleMedium)
                    Text("${stringResource(R.string.result_area)}: ${fmt(r.area, 3)} m² (${fmt(r.area * 10000, 0)} cm²)", style = MaterialTheme.typography.titleMedium)
                    if (r.sideMismatchPercent > 5.0) {
                        Text(
                            stringResource(R.string.photo_warn_sides, r.sideMismatchPercent.toInt()),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Text(stringResource(R.string.photo_accuracy_note), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ReferenceDialog(
    mode: PhotoMode,
    errorRes: Int?,
    onConfirm: (String, String, LengthUnit) -> Unit,
    onCancel: () -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(LengthUnit.CENTIMETER) }
    val isRect = mode == PhotoMode.RECTANGLE

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(stringResource(if (isRect) R.string.photo_ref_rect_title else R.string.photo_ref_length_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = unit == LengthUnit.CENTIMETER,
                        onClick = { unit = LengthUnit.CENTIMETER },
                        label = { Text(stringResource(R.string.photo_unit_cm)) }
                    )
                    FilterChip(
                        selected = unit == LengthUnit.METER,
                        onClick = { unit = LengthUnit.METER },
                        label = { Text(stringResource(R.string.photo_unit_m)) }
                    )
                }
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it },
                    label = { Text(stringResource(if (isRect) R.string.photo_width_label else R.string.photo_length_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (isRect) {
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it },
                        label = { Text(stringResource(R.string.photo_height_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                if (errorRes != null) ErrorText(stringResource(errorRes))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(first, second, unit) }) { Text(stringResource(R.string.photo_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.photo_cancel)) }
        }
    )
}

@Composable
private fun SaveDialog(onConfirm: (String, MeasurementType) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MeasurementType.CUSTOM) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.photo_save)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.photo_project_name)) },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MeasurementType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(stringResource(typeLabel(t))) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, type) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.photo_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.photo_cancel)) }
        }
    )
}

private fun typeLabel(t: MeasurementType): Int = when (t) {
    MeasurementType.WALL -> R.string.type_wall
    MeasurementType.DOOR -> R.string.type_door
    MeasurementType.WINDOW -> R.string.type_window
    MeasurementType.CLOSET -> R.string.type_closet
    MeasurementType.ROOM -> R.string.type_room
    MeasurementType.CUSTOM -> R.string.type_custom
}
