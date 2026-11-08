package com.smartmeter3d.calculator

object InputParser {
    /** Kullanıcı girişini (virgül veya nokta ondalık) metreye çevirir. Geçersizse InvalidMeasurementException fırlatır. */
    fun parseLengthToMeters(text: String, unit: LengthUnit): Double {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty()) throw InvalidMeasurementException(MeasurementError.EMPTY_INPUT)
        val value = cleaned.toDoubleOrNull()
            ?: throw InvalidMeasurementException(MeasurementError.NOT_A_NUMBER)
        val meters = UnitConverter.lengthToMeters(value, unit)
        return MeasurementCalculator.validateDimension(meters)
    }
}
