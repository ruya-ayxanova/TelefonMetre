package com.smartmeter3d.ui

import androidx.annotation.StringRes
import com.smartmeter3d.R
import com.smartmeter3d.calculator.MeasurementError

@StringRes
fun MeasurementError.messageRes(): Int = when (this) {
    MeasurementError.EMPTY_INPUT -> R.string.err_empty_input
    MeasurementError.EMPTY_NAME -> R.string.err_empty_name
    MeasurementError.REFERENCE_REQUIRED -> R.string.err_reference
    MeasurementError.DEGENERATE_POINTS -> R.string.err_degenerate
    MeasurementError.POINTS_TOO_CLOSE -> R.string.err_close
    MeasurementError.NOT_A_NUMBER -> R.string.err_not_number
    MeasurementError.NOT_FINITE -> R.string.err_not_finite
    MeasurementError.NEGATIVE_VALUE -> R.string.err_negative
    MeasurementError.ZERO_VALUE -> R.string.err_zero
    MeasurementError.TOO_LARGE -> R.string.err_too_large
    MeasurementError.OPENINGS_EXCEED_WALL -> R.string.err_openings
}
