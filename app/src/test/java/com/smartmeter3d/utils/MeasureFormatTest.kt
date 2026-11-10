package com.smartmeter3d.utils

import com.smartmeter3d.calculator.UnitKind
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasureFormatTest {
    @Test fun length_showsMetersAndCentimeters() {
        assertEquals("1.500 m (150.0 cm)", MeasureFormat.length(1.5))
    }

    @Test fun area_showsSquareMetersAndSquareCentimeters() {
        assertEquals("12.000 m² (120000 cm²)", MeasureFormat.area(12.0))
    }

    @Test fun volume_andCount() {
        assertEquals("2.880 m³", MeasureFormat.volume(2.88))
        assertEquals("3", MeasureFormat.count(3.0))
        assertEquals("3", MeasureFormat.line(UnitKind.COUNT, 3.0))
    }
}
