package com.smartmeter3d.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEvaluatorTest {
    private val delta = 1e-9

    @Test fun area_andVolume() {
        val a = CalculatorEvaluator.area("200", "150", LengthUnit.CENTIMETER)
        assertEquals(3.0, a.lines.single().value, delta)
        val v = CalculatorEvaluator.volume("2", "3", "4", LengthUnit.METER)
        assertEquals(24.0, v.lines.single().value, delta)
    }

    @Test fun netWall_blankOpeningsCountAsZero() {
        val e = CalculatorEvaluator.netWall("12,5", "", "")
        assertEquals(12.5, e.lines.first { it.key == ResultKey.NET_AREA }.value, delta)
    }

    @Test fun netWall_subtractsDoorsAndWindows() {
        val e = CalculatorEvaluator.netWall("12.5", "1.89", "3.4")
        assertEquals(7.21, e.lines.first { it.key == ResultKey.NET_AREA }.value, 1e-9)
    }

    @Test fun netWall_openingsTooBigIsError() {
        assertEquals(MeasurementError.OPENINGS_EXCEED_WALL, CalculatorEvaluator.netWall("2", "1.5", "1").error)
    }

    @Test fun convert_lengthAndArea() {
        assertEquals(1.5, CalculatorEvaluator.convertLength("150", LengthUnit.CENTIMETER).lines.single().value, delta)
        assertEquals(1.0, CalculatorEvaluator.convertArea("10000", AreaUnit.SQUARE_CENTIMETER).lines.single().value, delta)
    }

    @Test fun incompleteAndInvalid() {
        val incomplete = CalculatorEvaluator.area("200", "", LengthUnit.CENTIMETER)
        assertTrue(incomplete.lines.isEmpty())
        assertEquals(null, incomplete.error)
        assertEquals(MeasurementError.NOT_A_NUMBER, CalculatorEvaluator.convertArea("abc", AreaUnit.SQUARE_METER).error)
        assertEquals(MeasurementError.NEGATIVE_VALUE, CalculatorEvaluator.convertArea("-3", AreaUnit.SQUARE_METER).error)
        assertEquals(MeasurementError.TOO_LARGE, CalculatorEvaluator.convertArea("9999999", AreaUnit.SQUARE_METER).error)
    }
}
