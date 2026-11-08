package com.smartmeter3d.calculator

object InputParser {
    /** Kullanıcı girişini (virgül/nokta ondalık, Latin/Fars/Arap rakamları) metreye çevirir. Geçersizse InvalidMeasurementException fırlatır. */
    fun parseLengthToMeters(text: String, unit: LengthUnit): Double {
        val cleaned = normalize(text)
        if (cleaned.isEmpty()) throw InvalidMeasurementException(MeasurementError.EMPTY_INPUT)
        val value = cleaned.toDoubleOrNull()
            ?: throw InvalidMeasurementException(MeasurementError.NOT_A_NUMBER)
        val meters = UnitConverter.lengthToMeters(value, unit)
        return MeasurementCalculator.validateDimension(meters)
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
