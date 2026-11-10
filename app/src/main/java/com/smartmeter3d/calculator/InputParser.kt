package com.smartmeter3d.calculator

object InputParser {
    const val MAX_AREA_SQUARE_METERS = 1_000_000.0

    /** Kullanıcı girişini (virgül/nokta ondalık, Latin/Fars/Arap rakamları) metreye çevirir. Geçersizse InvalidMeasurementException fırlatır. */
    fun parseLengthToMeters(text: String, unit: LengthUnit): Double {
        val cleaned = normalize(text)
        if (cleaned.isEmpty()) throw InvalidMeasurementException(MeasurementError.EMPTY_INPUT)
        val value = cleaned.toDoubleOrNull()
            ?: throw InvalidMeasurementException(MeasurementError.NOT_A_NUMBER)
        val meters = UnitConverter.lengthToMeters(value, unit)
        return MeasurementCalculator.validateDimension(meters)
    }

    /** Alan girişini metrekareye çevirir. [allowZero] true ise 0 kabul edilir (ör. kapı/pencere yok). */
    fun parseAreaToSquareMeters(text: String, unit: AreaUnit, allowZero: Boolean): Double {
        val cleaned = normalize(text)
        if (cleaned.isEmpty()) throw InvalidMeasurementException(MeasurementError.EMPTY_INPUT)
        val value = cleaned.toDoubleOrNull()
            ?: throw InvalidMeasurementException(MeasurementError.NOT_A_NUMBER)
        if (value.isNaN()) throw InvalidMeasurementException(MeasurementError.NOT_A_NUMBER)
        if (value.isInfinite()) throw InvalidMeasurementException(MeasurementError.NOT_FINITE)
        val squareMeters = UnitConverter.areaToSquareMeters(value, unit)
        if (squareMeters < 0.0) throw InvalidMeasurementException(MeasurementError.NEGATIVE_VALUE)
        if (squareMeters == 0.0 && !allowZero) throw InvalidMeasurementException(MeasurementError.ZERO_VALUE)
        if (squareMeters > MAX_AREA_SQUARE_METERS) throw InvalidMeasurementException(MeasurementError.TOO_LARGE)
        return squareMeters
    }

    private fun normalize(text: String): String = buildString {
        for (ch in text.trim()) {
            when (ch) {
                in '\u06F0'..'\u06F9' -> append('0' + (ch - '\u06F0'))
                in '\u0660'..'\u0669' -> append('0' + (ch - '\u0660'))
                '\u066B', ',' -> append('.')
                '\u066C' -> Unit
                else -> append(ch)
            }
        }
    }
}
