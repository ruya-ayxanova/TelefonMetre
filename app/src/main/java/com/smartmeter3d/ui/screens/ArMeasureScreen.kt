package com.smartmeter3d.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Paint
import android.opengl.GLSurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter3d.R
import com.smartmeter3d.ar.ArController
import com.smartmeter3d.ar.ArFrameState
import com.smartmeter3d.ar.ArIssue
import com.smartmeter3d.ar.ArSetupResult
import com.smartmeter3d.ar.SurfaceKind
import com.smartmeter3d.calculator.LengthUnit
import com.smartmeter3d.measurement.ArMath
import com.smartmeter3d.measurement.ArMode
import com.smartmeter3d.measurement.ArOutcome
import com.smartmeter3d.measurement.ArPointData
import com.smartmeter3d.ui.components.SaveMeasurementDialog
import com.smartmeter3d.ui.messageRes
import com.smartmeter3d.viewmodel.ArMeasureViewModel
import java.util.Locale

private class GlHolder { var view: GLSurfaceView? = null }

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun fmt(v: Double, digits: Int): String = String.format(Locale.US, "%.${digits}f", v)

private fun formatLength(meters: Double): String =
    if (meters >= 1.0) "${fmt(meters, 3)} m" else "${fmt(meters * 100, 1)} cm"

@StringRes
private fun setupMessage(result: ArSetupResult): Int = when (result) {
    ArSetupResult.NOT_SUPPORTED -> R.string.ar_err_not_supported
    ArSetupResult.NOT_INSTALLED -> R.string.ar_err_not_installed
    ArSetupResult.APK_TOO_OLD -> R.string.ar_err_apk_old
    ArSetupResult.SDK_TOO_OLD -> R.string.ar_err_sdk_old
    ArSetupResult.CAMERA_ERROR -> R.string.ar_err_camera
    ArSetupResult.INSTALL_REQUESTED -> R.string.ar_installing
    ArSetupResult.ERROR -> R.string.ar_err_generic
    ArSetupResult.READY -> R.string.ar_starting
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArMeasureScreen(onBack: () -> Unit, vm: ArMeasureViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { ArController(context.applicationContext) }
    val glHolder = remember { GlHolder() }

    var permissionGranted by remember { mutableStateOf(hasCameraPermission(context)) }
    var permissionAsked by remember { mutableStateOf(false) }
    var setup by remember { mutableStateOf<ArSetupResult?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        permissionAsked = true
    }

    DisposableEffect(controller) {
        controller.renderer.listener = { vm.onFrame(it) }
        onDispose {
            controller.renderer.listener = null
            glHolder.view?.onPause()
            controller.release()
        }
    }

    DisposableEffect(lifecycleOwner, permissionGranted) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (permissionGranted && activity != null) {
                        val result = controller.ensureSession(activity)
                        setup = if (result == ArSetupResult.READY && !controller.resume()) {
                            ArSetupResult.CAMERA_ERROR
                        } else {
                            result
                        }
                        glHolder.view?.onResume()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    glHolder.view?.onPause()
                    controller.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_ar)) },
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
        Box(Modifier.fillMaxSize().padding(inner)) {
            val currentSetup = setup
            when {
                !permissionGranted -> MessagePane(
                    title = stringResource(R.string.ar_perm_title),
                    body = stringResource(if (permissionAsked) R.string.ar_perm_denied else R.string.ar_perm_body),
                    actionLabel = stringResource(R.string.ar_perm_grant),
                    onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
                currentSetup == null -> MessagePane(body = stringResource(R.string.ar_starting))
                currentSetup != ArSetupResult.READY -> MessagePane(
                    body = stringResource(setupMessage(currentSetup)),
                    actionLabel = if (currentSetup == ArSetupResult.NOT_SUPPORTED) null else stringResource(R.string.ar_retry),
                    onAction = {
                        if (activity != null) {
                            val result = controller.ensureSession(activity)
                            setup = if (result == ArSetupResult.READY && !controller.resume()) ArSetupResult.CAMERA_ERROR else result
                        }
                    }
                )
                else -> ArLiveContent(vm, controller, glHolder, activity) { setup = it }
            }
        }
    }
}

@Composable
private fun MessagePane(
    body: String,
    title: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun ArLiveContent(
    vm: ArMeasureViewModel,
    controller: ArController,
    glHolder: GlHolder,
    activity: Activity?,
    onSetupChanged: (ArSetupResult) -> Unit
) {
    val frame by vm.frame.collectAsState()
    val mode = vm.mode
    val points = frame.points
    val evaluation = remember(points, mode) {
        ArMath.evaluate(mode, points.map { ArPointData(it.world, it.startsNewChain) })
    }
    val maxPoints = if (mode == ArMode.RECTANGLE) 3 else Int.MAX_VALUE
    val canPlace = frame.canPlace && points.size < maxPoints
    var showSave by remember { mutableStateOf(false) }
    var showCompare by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    preserveEGLContextOnPause = true
                    setEGLContextClientVersion(2)
                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                    setRenderer(controller.renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }.also { glHolder.view = it }
            },
            modifier = Modifier.fillMaxSize()
        )
        ArOverlay(frame, canPlace)

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == ArMode.DISTANCE,
                    onClick = { vm.selectMode(ArMode.DISTANCE); controller.clearPoints() },
                    label = { Text(stringResource(R.string.ar_mode_distance)) }
                )
                FilterChip(
                    selected = mode == ArMode.RECTANGLE,
                    onClick = { vm.selectMode(ArMode.RECTANGLE); controller.clearPoints() },
                    label = { Text(stringResource(R.string.ar_mode_rect)) }
                )
            }

            Text(stringResource(statusRes(frame)), style = MaterialTheme.typography.titleMedium)
            if (frame.tracking && frame.lowLight) {
                Text(stringResource(R.string.ar_low_light), color = MaterialTheme.colorScheme.error)
            }
            if (frame.tracking) {
                Text(stringResource(R.string.ar_planes, frame.planeCount, frame.wallCount), style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                stringResource(if (mode == ArMode.RECTANGLE) R.string.ar_hint_rect else R.string.ar_hint_distance),
                style = MaterialTheme.typography.bodyLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    enabled = canPlace,
                    onClick = {
                        vm.resetResultState()
                        controller.placePoint(vm.consumeNewChainFlag())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.AddLocation, contentDescription = null)
                    Text(stringResource(R.string.ar_add_point), modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = { vm.resetResultState(); controller.undo() }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                    Text(stringResource(R.string.photo_undo), modifier = Modifier.padding(start = 6.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.resetResultState(); controller.clearPoints() }) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(stringResource(R.string.photo_clear), modifier = Modifier.padding(start = 6.dp))
                }
                if (mode == ArMode.DISTANCE) {
                    OutlinedButton(onClick = { vm.requestNewChain() }) {
                        Text(stringResource(R.string.ar_new_edge))
                    }
                }
                OutlinedButton(onClick = {
                    if (activity != null) {
                        glHolder.view?.onPause()
                        val result = controller.restart(activity)
                        glHolder.view?.onResume()
                        vm.resetResultState()
                        if (result != ArSetupResult.READY) onSetupChanged(result)
                    }
                }) {
                    Text(stringResource(R.string.ar_recalibrate))
                }
            }

            val shownError = evaluation.error ?: vm.error
            if (shownError != null) {
                Text(stringResource(shownError.messageRes()), color = MaterialTheme.colorScheme.error)
            }

            val outcome = evaluation.outcome
            if (outcome != null) {
                ArResult(outcome)
                vm.comparison?.let { c ->
                    Text(
                        stringResource(
                            R.string.ar_compare_result,
                            formatLength(c.arMeters),
                            formatLength(c.manualMeters),
                            "${if (c.differenceMeters >= 0) "+" else "-"}${fmt(kotlin.math.abs(c.differenceMeters) * 100, 1)} cm",
                            "${if (c.percent >= 0) "+" else ""}${fmt(c.percent, 1)}"
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showSave = true }) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Text(stringResource(R.string.photo_save), modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = { vm.dismissError(); showCompare = true }) {
                        Text(stringResource(R.string.ar_compare))
                    }
                }
                if (vm.saved) Text(stringResource(R.string.photo_saved), color = MaterialTheme.colorScheme.primary)
            }
            Text(stringResource(R.string.ar_accuracy_note), style = MaterialTheme.typography.bodyLarge)
        }
    }

    val outcomeNow = evaluation.outcome
    if (showSave && outcomeNow != null) {
        SaveMeasurementDialog(
            onConfirm = { name, type ->
                showSave = false
                vm.save(name, type, outcomeNow)
            },
            onCancel = { showSave = false }
        )
    }
    if (showCompare && outcomeNow != null) {
        val arMeters = when (outcomeNow) {
            is ArOutcome.Distance -> outcomeNow.total
            is ArOutcome.Rect -> outcomeNow.result.width
        }
        CompareDialog(
            subjectRes = if (outcomeNow is ArOutcome.Distance) R.string.ar_compare_subject_total else R.string.ar_compare_subject_width,
            errorRes = vm.error?.messageRes(),
            onConfirm = { text, unit -> if (vm.compare(arMeters, text, unit)) showCompare = false },
            onCancel = { showCompare = false }
        )
    }
}

@StringRes
private fun statusRes(f: ArFrameState): Int = when {
    !f.tracking -> when (f.issue) {
        ArIssue.INSUFFICIENT_LIGHT -> R.string.ar_issue_light
        ArIssue.EXCESSIVE_MOTION -> R.string.ar_issue_motion
        ArIssue.INSUFFICIENT_FEATURES -> R.string.ar_issue_features
        ArIssue.CAMERA_UNAVAILABLE -> R.string.ar_issue_camera
        else -> R.string.ar_status_initializing
    }
    f.crosshairSurface == SurfaceKind.NONE -> R.string.ar_no_surface
    f.crosshairSurface == SurfaceKind.WALL -> R.string.ar_ready_wall
    f.crosshairSurface == SurfaceKind.FLOOR -> R.string.ar_ready_floor
    else -> R.string.ar_ready_ceiling
}

@Composable
private fun ArResult(outcome: ArOutcome) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (outcome) {
            is ArOutcome.Distance -> {
                outcome.edges.forEachIndexed { i, e ->
                    Text("${stringResource(R.string.ar_edge, i + 1)}: ${fmt(e, 3)} m (${fmt(e * 100, 1)} cm)", style = MaterialTheme.typography.titleMedium)
                }
                if (outcome.edges.size > 1) {
                    Text("${stringResource(R.string.ar_total)}: ${fmt(outcome.total, 3)} m (${fmt(outcome.total * 100, 1)} cm)", style = MaterialTheme.typography.titleLarge)
                }
            }
            is ArOutcome.Rect -> {
                val r = outcome.result
                Text("${stringResource(R.string.result_width)}: ${fmt(r.width, 3)} m (${fmt(r.width * 100, 1)} cm)", style = MaterialTheme.typography.titleMedium)
                Text("${stringResource(R.string.result_height)}: ${fmt(r.height, 3)} m (${fmt(r.height * 100, 1)} cm)", style = MaterialTheme.typography.titleMedium)
                Text("${stringResource(R.string.result_area)}: ${fmt(r.area, 3)} m² (${fmt(r.area * 10000, 0)} cm²)", style = MaterialTheme.typography.titleLarge)
                if (kotlin.math.abs(r.cornerAngleDegrees - 90.0) > 10.0) {
                    Text(
                        stringResource(R.string.ar_angle_warn, r.cornerAngleDegrees.toInt()),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ArOverlay(frame: ArFrameState, canPlace: Boolean) {
    val density = LocalDensity.current
    val paint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 14.sp.toPx() }
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val crossColor = if (canPlace) Color(0xFF00E676) else Color(0xFFFF5252)
        val r = 18.dp.toPx()
        val w = 2.dp.toPx()
        drawCircle(crossColor, r, center, style = Stroke(width = w))
        drawLine(crossColor, Offset(center.x - r * 1.6f, center.y), Offset(center.x + r * 1.6f, center.y), strokeWidth = w)
        drawLine(crossColor, Offset(center.x, center.y - r * 1.6f), Offset(center.x, center.y + r * 1.6f), strokeWidth = w)

        val pts = frame.points
        for (i in 1 until pts.size) {
            if (pts[i].startsNewChain) continue
            val a = pts[i - 1]
            val b = pts[i]
            if (!a.onScreen || !b.onScreen) continue
            drawLine(Color(0xFFFFEB3B), Offset(a.screenX, a.screenY), Offset(b.screenX, b.screenY), strokeWidth = 3.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                formatLength(ArMath.distance(a.world, b.world)),
                (a.screenX + b.screenX) / 2f + 6.dp.toPx(),
                (a.screenY + b.screenY) / 2f - 6.dp.toPx(),
                paint
            )
        }
        pts.forEach { p ->
            if (p.onScreen) {
                drawCircle(Color.White, 9.dp.toPx(), Offset(p.screenX, p.screenY))
                drawCircle(Color(0xFFFFC107), 6.dp.toPx(), Offset(p.screenX, p.screenY))
            }
        }
    }
}

@Composable
private fun CompareDialog(
    @StringRes subjectRes: Int,
    errorRes: Int?,
    onConfirm: (String, LengthUnit) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(LengthUnit.CENTIMETER) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.ar_compare_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(subjectRes), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.ar_compare_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (errorRes != null) Text(stringResource(errorRes), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text, unit) }) { Text(stringResource(R.string.photo_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.photo_cancel)) }
        }
    )
}
