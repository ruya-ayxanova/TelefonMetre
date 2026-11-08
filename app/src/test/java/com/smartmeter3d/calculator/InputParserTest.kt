package com.smartmeter3d.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InputParserTest {
    private val delta = 1e-9

    private fun errorOf(text: String, unit: LengthUnit = LengthUnit.METER): MeasurementError =
        assertThrows(InvalidMeasurementException::class.java) {
            InputParser.parseLengthToMeters(text, unit)
        }.error

    @Test fun parsesDotAndCommaDecimals() {
        assertEquals(1.5, InputParser.parseLengthToMeters("1.5", LengthUnit.METER), delta)
        assertEquals(1.5, InputParser.parseLengthToMeters("1,5", LengthUnit.METER), delta)
        assertEquals(1.5, InputParser.parseLengthToMeters(" 1,5 ", LengthUnit.METER), delta)
    }

    @Test fun convertsCentimetersToMeters() {
        assertEquals(2.1, InputParser.parseLengthToMeters("210", LengthUnit.CENTIMETER), delta)
    }

    @Test fun rejectsEmptyAndNonNumeric() {
        assertEquals(MeasurementError.EMPTY_INPUT, errorOf(""))
        assertEquals(MeasurementError.EMPTY_INPUT, errorOf("   "))
        assertEquals(MeasurementError.NOT_A_NUMBER, errorOf("abc"))
        assertEquals(MeasurementError.NOT_A_NUMBER, errorOf("1.2.3"))
    }

    @Test fun rejectsNegativeZeroAndHuge() {
        assertEquals(MeasurementError.NEGATIVE_VALUE, errorOf("-2"))
        assertEquals(MeasurementError.ZERO_VALUE, errorOf("0"))
        assertEquals(MeasurementError.TOO_LARGE, errorOf("2000"))
    }
}
