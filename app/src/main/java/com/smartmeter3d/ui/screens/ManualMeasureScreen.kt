package com.smartmeter3d.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.smartmeter3d.calculator.Field
import com.smartmeter3d.calculator.LengthUnit
import com.smartmeter3d.calculator.ManualCalculations
import com.smartmeter3d.calculator.ManualEvaluator
import com.smartmeter3d.database.MeasurementType
import com.smartmeter3d.ui.components.SaveMeasurementDialog
import com.smartmeter3d.ui.labelRes
import com.smartmeter3d.ui.messageRes
import com.smartmeter3d.ui.titleRes
import com.smartmeter3d.utils.MeasureFormat
import com.smartmeter3d.viewmodel.ManualMeasureViewModel

private data class OpeningText(val w: String = "", val h: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualMeasureScreen(
    type: MeasurementType,
    onBack: () -> Unit,
    onOpenAr: () -> Unit,
    onOpenPhoto: () -> Unit,
    vm: ManualMeasureViewModel = viewModel()
) {
    val texts = remember { mutableStateMapOf<Field, String>() }
    val doors = remember { mutableStateListOf<OpeningText>() }
    val windows = remember { mutableStateListOf<OpeningText>() }
    var unit by remember { mutableStateOf(LengthUnit.CENTIMETER) }
    var showSave by remember { mutableStateOf(false) }

    val required = ManualCalculations.requiredFields(type)
    val optional = ManualCalculations.optionalFields(type)
    val evaluation = ManualEvaluator.evaluate(
        type, texts.toMap(), unit,
        doors.map { it.w to it.h }, windows.map { it.w to it.h }
    )
    val result = evaluation.result

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(type.titleRes())) },
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
                OutlinedButton(onClick = onOpenAr, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.manual_measure_ar))
                }
                OutlinedButton(onClick = onOpenPhoto, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.manual_measure_photo))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = unit == LengthUnit.CENTIMETER,
                    onClick = { unit = LengthUnit.CENTIMETER; vm.clearStatus() },
                    label = { Text(stringResource(R.string.photo_unit_cm)) }
                )
                FilterChip(
                    selected = unit == LengthUnit.METER,
                    onClick = { unit = LengthUnit.METER; vm.clearStatus() },
                    label = { Text(stringResource(R.string.photo_unit_m)) }
                )
            }

            Text(stringResource(R.string.manual_prompt), style = MaterialTheme.typography.bodyLarge)

            (required.map { it to false } + optional.map { it to true }).forEach { (field, isOptional) ->
                OutlinedTextField(
                    value = texts[field].orEmpty(),
                    onValueChange = { texts[field] = it; vm.clearStatus() },
                    label = { Text(stringResource(field.labelRes(isOptional))) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (ManualCalculations.supportsOpenings(type)) {
                OpeningSection(R.string.manual_doors, R.string.manual_add_door, doors) { vm.clearStatus() }
                OpeningSection(R.string.manual_windows, R.string.manual_add_window, windows) { vm.clearStatus() }
            }

            evaluation.error?.let {
                Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }

            if (result != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.lines.forEach { line ->
                            Text(
                                "${stringResource(line.key.labelRes())}: ${MeasureFormat.line(line.kind, line.value)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                Button(onClick = { showSave = true }) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Text(stringResource(R.string.photo_save), modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (vm.saved) Text(stringResource(R.string.photo_saved), color = MaterialTheme.colorScheme.primary)
            vm.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        }
    }

    if (showSave && result != null) {
        SaveMeasurementDialog(
            defaultType = type,
            onConfirm = { name, savedType ->
                showSave = false
                vm.save(name, savedType, result, unit)
            },
            onCancel = { showSave = false }
        )
    }
}

@Composable
private fun OpeningSection(
    titleRes: Int,
    addRes: Int,
    rows: MutableList<OpeningText>,
    onChanged: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        rows.forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.w,
                    onValueChange = { rows[index] = row.copy(w = it); onChanged() },
                    label = { Text(stringResource(R.string.manual_opening_width)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = row.h,
                    onValueChange = { rows[index] = row.copy(h = it); onChanged() },
                    label = { Text(stringResource(R.string.manual_opening_height)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { rows.removeAt(index); onChanged() }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.manual_remove))
                }
            }
        }
        OutlinedButton(onClick = { rows.add(OpeningText()); onChanged() }) {
            Text(stringResource(addRes))
        }
    }
}
