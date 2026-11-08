package com.smartmeter3d.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class InputParserDigitsTest {
    private val delta = 1e-9

    @Test fun parsesPersianDigitsAndDecimalSeparator() {
        assertEquals(1.5, InputParser.parseLengthToMeters("۱٫۵", LengthUnit.METER), delta)
        assertEquals(2.1, InputParser.parseLengthToMeters("۲۱۰", LengthUnit.CENTIMETER), delta)
    }

    @Test fun parsesArabicIndicDigits() {
        assertEquals(2.0, InputParser.parseLengthToMeters("٢٠٠", LengthUnit.CENTIMETER), delta)
    }
}
