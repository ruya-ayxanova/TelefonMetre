package com.smartmeter3d.utils

import com.smartmeter3d.calculator.AreaUnit
import com.smartmeter3d.calculator.LengthUnit
import com.smartmeter3d.calculator.UnitConverter
import com.smartmeter3d.calculator.UnitKind
import java.util.Locale

/** Değerler metre / m² / m³ olarak saklanır; birim dönüşümü yalnızca burada, gösterim sırasında yapılır. */
object MeasureFormat {
    private fun f(v: Double, digits: Int): String = String.format(Locale.US, "%.${digits}f", v)

    fun length(meters: Double): String =
        "${f(meters, 3)} m (${f(UnitConverter.metersToLength(meters, LengthUnit.CENTIMETER), 1)} cm)"

    fun area(squareMeters: Double): String =
        "${f(squareMeters, 3)} m² (${f(UnitConverter.squareMetersToArea(squareMeters, AreaUnit.SQUARE_CENTIMETER), 0)} cm²)"

    fun volume(cubicMeters: Double): String = "${f(cubicMeters, 3)} m³"

    fun count(value: Double): String = value.toInt().toString()

    fun line(kind: UnitKind, value: Double): String = when (kind) {
        UnitKind.LENGTH -> length(value)
        UnitKind.AREA -> area(value)
        UnitKind.VOLUME -> volume(value)
        UnitKind.COUNT -> count(value)
    }
}
