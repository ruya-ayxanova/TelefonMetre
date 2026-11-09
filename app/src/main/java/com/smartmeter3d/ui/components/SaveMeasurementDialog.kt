package com.smartmeter3d.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartmeter3d.R
import com.smartmeter3d.database.MeasurementType

@StringRes
fun MeasurementType.labelRes(): Int = when (this) {
    MeasurementType.WALL -> R.string.type_wall
    MeasurementType.DOOR -> R.string.type_door
    MeasurementType.WINDOW -> R.string.type_window
    MeasurementType.CLOSET -> R.string.type_closet
    MeasurementType.ROOM -> R.string.type_room
    MeasurementType.CUSTOM -> R.string.type_custom
}

@Composable
fun SaveMeasurementDialog(onConfirm: (String, MeasurementType) -> Unit, onCancel: () -> Unit) {
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
                            label = { Text(stringResource(t.labelRes())) }
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
