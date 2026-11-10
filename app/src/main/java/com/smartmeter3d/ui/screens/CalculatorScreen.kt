package com.smartmeter3d.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartmeter3d.R
import com.smartmeter3d.calculator.AreaUnit
import com.smartmeter3d.calculator.CalcEvaluation
import com.smartmeter3d.calculator.CalculatorEvaluator
import com.smartmeter3d.calculator.LengthUnit
import com.smartmeter3d.ui.labelRes
import com.smartmeter3d.ui.messageRes
import com.smartmeter3d.utils.MeasureFormat

private enum class CalcMode { AREA, VOLUME, NET, CONVERT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(CalcMode.AREA) }
    var f1 by remember { mutableStateOf("") }
    var f2 by remember { mutableStateOf("") }
    var f3 by remember { mutableStateOf("") }
    var lengthUnit by remember { mutableStateOf(LengthUnit.CENTIMETER) }
    var areaUnit by remember { mutableStateOf(AreaUnit.SQUARE_METER) }
    var convertArea by remember { mutableStateOf(false) }

    fun selectMode(m: CalcMode) {
        mode = m
        f1 = ""; f2 = ""; f3 = ""
    }

    val evaluation: CalcEvaluation = when (mode) {
        CalcMode.AREA -> CalculatorEvaluator.area(f1, f2, lengthUnit)
        CalcMode.VOLUME -> CalculatorEvaluator.volume(f1, f2, f3, lengthUnit)
        CalcMode.NET -> CalculatorEvaluator.netWall(f1, f2, f3)
        CalcMode.CONVERT ->
            if (convertArea) CalculatorEvaluator.convertArea(f1, areaUnit)
            else CalculatorEvaluator.convertLength(f1, lengthUnit)
    }

    val labels: List<Int> = when (mode) {
        CalcMode.AREA -> listOf(R.string.field_width, R.string.field_height)
        CalcMode.VOLUME -> listOf(R.string.field_length, R.string.field_width, R.string.field_height)
        CalcMode.NET -> listOf(R.string.calc_wall_area_label, R.string.calc_door_area_label, R.string.calc_window_area_label)
        CalcMode.CONVERT -> listOf(R.string.calc_value_label)
    }
    val values = listOf(f1, f2, f3)
    val setters: List<(String) -> Unit> = listOf({ f1 = it }, { f2 = it }, { f3 = it })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_calculator)) },
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
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(mode == CalcMode.AREA, { selectMode(CalcMode.AREA) }, { Text(stringResource(R.string.calc_area)) })
                FilterChip(mode == CalcMode.VOLUME, { selectMode(CalcMode.VOLUME) }, { Text(stringResource(R.string.calc_volume)) })
                FilterChip(mode == CalcMode.NET, { selectMode(CalcMode.NET) }, { Text(stringResource(R.string.calc_net)) })
                FilterChip(mode == CalcMode.CONVERT, { selectMode(CalcMode.CONVERT) }, { Text(stringResource(R.string.calc_convert)) })
            }

            if (mode == CalcMode.CONVERT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!convertArea, { convertArea = false; f1 = "" }, { Text(stringResource(R.string.calc_kind_length)) })
                    FilterChip(convertArea, { convertArea = true; f1 = "" }, { Text(stringResource(R.string.calc_kind_area)) })
                }
            }

            if (mode == CalcMode.AREA || mode == CalcMode.VOLUME || (mode == CalcMode.CONVERT && !convertArea)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(lengthUnit == LengthUnit.CENTIMETER, { lengthUnit = LengthUnit.CENTIMETER }, { Text(stringResource(R.string.photo_unit_cm)) })
                    FilterChip(lengthUnit == LengthUnit.METER, { lengthUnit = LengthUnit.METER }, { Text(stringResource(R.string.photo_unit_m)) })
                }
            }
            if (mode == CalcMode.CONVERT && convertArea) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(areaUnit == AreaUnit.SQUARE_METER, { areaUnit = AreaUnit.SQUARE_METER }, { Text(stringResource(R.string.unit_m2)) })
                    FilterChip(areaUnit == AreaUnit.SQUARE_CENTIMETER, { areaUnit = AreaUnit.SQUARE_CENTIMETER }, { Text(stringResource(R.string.unit_cm2)) })
                }
            }

            Text(stringResource(R.string.calc_prompt), style = MaterialTheme.typography.bodyLarge)

            labels.forEachIndexed { i, labelRes ->
                OutlinedTextField(
                    value = values[i],
                    onValueChange = setters[i],
                    label = { Text(stringResource(labelRes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            evaluation.error?.let {
                Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }
            if (evaluation.lines.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        evaluation.lines.forEach { line ->
                            Text(
                                "${stringResource(line.key.labelRes())}: ${MeasureFormat.line(line.kind, line.value)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
