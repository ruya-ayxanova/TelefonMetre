package com.smartmeter3d.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter3d.R
import com.smartmeter3d.calculator.UnitKind
import com.smartmeter3d.database.MeasurementEntity
import com.smartmeter3d.database.MeasurementType
import com.smartmeter3d.ui.components.labelRes
import com.smartmeter3d.ui.messageRes
import com.smartmeter3d.ui.methodLabelRes
import com.smartmeter3d.utils.ImageLoader
import com.smartmeter3d.utils.MeasureFormat
import com.smartmeter3d.viewmodel.ProjectsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(millis))

/** Kayıtlı alanlar: (etiket, değer). */
private fun valueLines(e: MeasurementEntity): List<Pair<Int, String>> {
    val out = ArrayList<Pair<Int, String>>()
    e.length?.let { out.add(R.string.field_length to MeasureFormat.line(UnitKind.LENGTH, it)) }
    e.width?.let { out.add(R.string.field_width to MeasureFormat.line(UnitKind.LENGTH, it)) }
    e.height?.let { out.add(R.string.field_height to MeasureFormat.line(UnitKind.LENGTH, it)) }
    e.depth?.let { out.add(R.string.field_depth to MeasureFormat.line(UnitKind.LENGTH, it)) }
    e.area?.let { out.add(R.string.field_area to MeasureFormat.line(UnitKind.AREA, it)) }
    e.volume?.let { out.add(R.string.field_volume to MeasureFormat.line(UnitKind.VOLUME, it)) }
    return out
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(onBack: () -> Unit, vm: ProjectsViewModel = viewModel()) {
    val items by vm.items.collectAsState()
    val filter by vm.filter.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var deletingId by remember { mutableStateOf<Long?>(null) }

    val selected = items.firstOrNull { it.id == selectedId }
    val editing = items.firstOrNull { it.id == editingId }
    val deleting = items.firstOrNull { it.id == deletingId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_projects)) },
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
        Column(Modifier.fillMaxSize().padding(inner)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { vm.selectFilter(null) },
                    label = { Text(stringResource(R.string.projects_all)) }
                )
                MeasurementType.values().forEach { t ->
                    FilterChip(
                        selected = filter == t,
                        onClick = { vm.selectFilter(t) },
                        label = { Text(stringResource(t.labelRes())) }
                    )
                }
            }

            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.projects_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { e ->
                        ProjectCard(e) { selectedId = e.id }
                    }
                }
            }
        }
    }

    if (selected != null) {
        DetailDialog(
            entity = selected,
            onEdit = { editingId = selected.id },
            onDelete = { deletingId = selected.id },
            onClose = { selectedId = null }
        )
    }
    if (editing != null) {
        EditDialog(
            entity = editing,
            errorRes = vm.error?.messageRes(),
            onConfirm = { name, type -> vm.update(editing, name, type) { editingId = null } },
            onCancel = { vm.clearError(); editingId = null }
        )
    }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text(deleting.projectName) },
            text = { Text(stringResource(R.string.projects_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(deleting)
                    deletingId = null
                    selectedId = null
                }) { Text(stringResource(R.string.projects_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.photo_cancel)) }
            }
        )
    }
}

@Composable
private fun ProjectCard(e: MeasurementEntity, onClick: () -> Unit) {
    val summary = e.area?.let { MeasureFormat.line(UnitKind.AREA, it) }
        ?: e.length?.let { MeasureFormat.line(UnitKind.LENGTH, it) }
        ?: e.volume?.let { MeasureFormat.line(UnitKind.VOLUME, it) }
        ?: ""
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(e.projectName, style = MaterialTheme.typography.titleMedium)
            Text(
                "${stringResource(e.type.labelRes())} · ${stringResource(e.method.methodLabelRes())} · ${formatDate(e.createdAt)}",
                style = MaterialTheme.typography.bodyLarge
            )
            if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DetailDialog(entity: MeasurementEntity, onEdit: () -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    val photo by produceState<Bitmap?>(initialValue = null, entity.photoPath) {
        val path = entity.photoPath
        value = if (path == null) null else withContext(Dispatchers.IO) { ImageLoader.load(path, 1024) }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(entity.projectName) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${stringResource(R.string.projects_type)}: ${stringResource(entity.type.labelRes())}")
                Text("${stringResource(R.string.projects_method)}: ${stringResource(entity.method.methodLabelRes())}")
                Text("${stringResource(R.string.projects_date)}: ${formatDate(entity.createdAt)}")
                valueLines(entity).forEach { (label, value) ->
                    Text("${stringResource(label)}: $value", style = MaterialTheme.typography.titleMedium)
                }
                photo?.let {
                    Text(stringResource(R.string.projects_photo), style = MaterialTheme.typography.titleMedium)
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.projects_edit)) }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.projects_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.projects_close)) }
        }
    )
}

@Composable
private fun EditDialog(
    entity: MeasurementEntity,
    errorRes: Int?,
    onConfirm: (String, MeasurementType) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(entity.projectName) }
    var type by remember { mutableStateOf(entity.type) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.projects_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.projects_edit_note), style = MaterialTheme.typography.bodyLarge)
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
                if (errorRes != null) Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, type) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.projects_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.photo_cancel)) }
        }
    )
}
