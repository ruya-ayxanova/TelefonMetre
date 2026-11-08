package com.smartmeter3d.calculator

/** Tüm hesaplamalar metre tabanlıdır; birim dönüşümü yalnızca girişte/gösterimde uygulanır. */
enum class LengthUnit(val toMeters: Double) {
    METER(1.0),
    CENTIMETER(0.01)
}

enum class AreaUnit(val toSquareMeters: Double) {
    SQUARE_METER(1.0),
    SQUARE_CENTIMETER(0.0001)
}

enum class VolumeUnit(val toCubicMeters: Double) {
    CUBIC_METER(1.0),
    CUBIC_CENTIMETER(0.000001)
}

object UnitConverter {
    fun lengthToMeters(value: Double, unit: LengthUnit): Double = value * unit.toMeters
    fun metersToLength(meters: Double, unit: LengthUnit): Double = meters / unit.toMeters

    fun areaToSquareMeters(value: Double, unit: AreaUnit): Double = value * unit.toSquareMeters
    fun squareMetersToArea(squareMeters: Double, unit: AreaUnit): Double = squareMeters / unit.toSquareMeters

    fun volumeToCubicMeters(value: Double, unit: VolumeUnit): Double = value * unit.toCubicMeters
    fun cubicMetersToVolume(cubicMeters: Double, unit: VolumeUnit): Double = cubicMeters / unit.toCubicMeters
}
