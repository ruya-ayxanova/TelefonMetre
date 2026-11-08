package com.smartmeter3d.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MeasurementCalculatorTest {
    private val delta = 1e-9

    private fun errorOf(block: () -> Unit): MeasurementError =
        assertThrows(InvalidMeasurementException::class.java) { block() }.error

    @Test fun rectangleArea_multipliesWidthAndHeight() {
        assertEquals(6.0, MeasurementCalculator.rectangleArea(2.0, 3.0), delta)
        assertEquals(0.375, MeasurementCalculator.rectangleArea(0.5, 0.75), delta)
    }

    @Test fun volume_multipliesThreeDimensions() {
        assertEquals(24.0, MeasurementCalculator.volume(2.0, 3.0, 4.0), delta)
    }

    @Test fun netWallArea_subtractsDoorsAndWindows() {
        // 5 x 2.5 = 12.5 ; kapı 0.9x2.1 = 1.89 ; pencere 1.2x1.0 = 1.2
        val net = MeasurementCalculator.netWallArea(12.5, listOf(1.89), listOf(1.2))
        assertEquals(9.41, net, 1e-9)
    }

    @Test fun netWallArea_noOpeningsEqualsGross() {
        assertEquals(10.0, MeasurementCalculator.netWallArea(10.0, emptyList(), emptyList()), delta)
    }

    @Test fun netWallArea_openingsLargerThanWallFails() {
        assertEquals(MeasurementError.OPENINGS_EXCEED_WALL, errorOf {
            MeasurementCalculator.netWallArea(2.0, listOf(1.5), listOf(1.0))
        })
    }

    @Test fun wall_computesCountsAndNetArea() {
        val r = MeasurementCalculator.wall(
            length = 5.0, height = 2.5,
            doors = listOf(Opening(0.9, 2.1)),
            windows = listOf(Opening(1.2, 1.0), Opening(1.0, 1.0))
        )
        assertEquals(12.5, r.grossArea, delta)
        assertEquals(1.89, r.doorArea, delta)
        assertEquals(2.2, r.windowArea, delta)
        assertEquals(8.41, r.netArea, 1e-9)
        assertEquals(1, r.doorCount)
        assertEquals(2, r.windowCount)
    }

    @Test fun closet_frontAreaAndVolume() {
        val r = MeasurementCalculator.closet(width = 2.0, height = 2.4, depth = 0.6)
        assertEquals(4.8, r.frontArea, delta)
        assertEquals(2.88, r.volume, 1e-9)
    }

    @Test fun room_floorWallsAndVolume() {
        val r = MeasurementCalculator.room(length = 4.0, width = 3.0, height = 2.5)
        assertEquals(12.0, r.floorArea, delta)
        assertEquals(listOf(10.0, 7.5, 10.0, 7.5), r.wallAreas)
        assertEquals(35.0, r.totalWallArea, delta)
        assertEquals(30.0, r.volume, delta)
    }

    @Test fun lengthConversion_meterAndCentimeter() {
        assertEquals(1.5, UnitConverter.lengthToMeters(150.0, LengthUnit.CENTIMETER), delta)
        assertEquals(250.0, UnitConverter.metersToLength(2.5, LengthUnit.CENTIMETER), delta)
        assertEquals(2.0, UnitConverter.lengthToMeters(2.0, LengthUnit.METER), delta)
    }

    @Test fun areaConversion_squareMeterToSquareCentimeter() {
        assertEquals(10_000.0, UnitConverter.squareMetersToArea(1.0, AreaUnit.SQUARE_CENTIMETER), delta)
        assertEquals(1.0, UnitConverter.areaToSquareMeters(10_000.0, AreaUnit.SQUARE_CENTIMETER), delta)
    }

    @Test fun volumeConversion_cubicMeterToCubicCentimeter() {
        assertEquals(1_000_000.0, UnitConverter.cubicMetersToVolume(1.0, VolumeUnit.CUBIC_CENTIMETER), 1e-6)
    }

    @Test fun invalidDimensions_areRejected() {
        assertEquals(MeasurementError.NEGATIVE_VALUE, errorOf { MeasurementCalculator.rectangleArea(-1.0, 2.0) })
        assertEquals(MeasurementError.ZERO_VALUE, errorOf { MeasurementCalculator.rectangleArea(0.0, 2.0) })
        assertEquals(MeasurementError.NOT_A_NUMBER, errorOf { MeasurementCalculator.volume(Double.NaN, 1.0, 1.0) })
        assertEquals(MeasurementError.NOT_FINITE, errorOf { MeasurementCalculator.volume(1.0, Double.POSITIVE_INFINITY, 1.0) })
        assertEquals(MeasurementError.TOO_LARGE, errorOf { MeasurementCalculator.rectangleArea(5000.0, 1.0) })
    }
}
