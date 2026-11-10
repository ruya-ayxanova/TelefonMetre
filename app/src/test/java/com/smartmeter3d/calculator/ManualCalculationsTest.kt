package com.smartmeter3d.calculator

import com.smartmeter3d.database.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualCalculationsTest {
    private val delta = 1e-9

    private fun eval(
        type: MeasurementType,
        texts: Map<Field, String>,
        doors: List<Pair<String, String>> = emptyList(),
        windows: List<Pair<String, String>> = emptyList(),
        unit: LengthUnit = LengthUnit.CENTIMETER
    ) = ManualEvaluator.evaluate(type, texts, unit, doors, windows)

    private fun ManualResult.value(key: ResultKey): Double = lines.first { it.key == key }.value

    @Test fun wall_netAreaWithDoorsAndWindows() {
        val e = eval(
            MeasurementType.WALL,
            mapOf(Field.LENGTH to "500", Field.HEIGHT to "250"),
            doors = listOf("90" to "210"),
            windows = listOf("120" to "100", "100" to "100")
        )
        val r = e.result!!
        assertNull(e.error)
        assertEquals(12.5, r.value(ResultKey.GROSS_AREA), delta)
        assertEquals(1.0, r.value(ResultKey.DOOR_COUNT), delta)
        assertEquals(2.0, r.value(ResultKey.WINDOW_COUNT), delta)
        assertEquals(8.41, r.value(ResultKey.NET_AREA), 1e-9)
        assertEquals(8.41, r.area!!, 1e-9)
        assertEquals(5.0, r.length!!, delta)
    }

    @Test fun wall_openingsLargerThanWallIsAnError() {
        val e = eval(
            MeasurementType.WALL,
            mapOf(Field.LENGTH to "100", Field.HEIGHT to "100"),
            doors = listOf("90" to "210")
        )
        assertNull(e.result)
        assertEquals(MeasurementError.OPENINGS_EXCEED_WALL, e.error)
    }

    @Test fun door_andWindow_area() {
        val d = eval(MeasurementType.DOOR, mapOf(Field.WIDTH to "90", Field.HEIGHT to "210")).result!!
        assertEquals(1.89, d.area!!, 1e-9)
        val w = eval(MeasurementType.WINDOW, mapOf(Field.WIDTH to "1,2", Field.HEIGHT to "1"), unit = LengthUnit.METER).result!!
        assertEquals(1.2, w.area!!, 1e-9)
    }

    @Test fun closet_frontAreaAndVolume() {
        val r = eval(MeasurementType.CLOSET, mapOf(Field.WIDTH to "200", Field.HEIGHT to "240", Field.DEPTH to "60")).result!!
        assertEquals(4.8, r.value(ResultKey.FRONT_AREA), 1e-9)
        assertEquals(2.88, r.value(ResultKey.VOLUME_APPROX), 1e-9)
        assertEquals(2.88, r.volume!!, 1e-9)
    }

    @Test fun room_floorWallsVolume() {
        val r = eval(MeasurementType.ROOM, mapOf(Field.LENGTH to "400", Field.WIDTH to "300", Field.HEIGHT to "250")).result!!
        assertEquals(12.0, r.value(ResultKey.FLOOR_AREA), 1e-9)
        assertEquals(10.0, r.value(ResultKey.WALL_A), 1e-9)
        assertEquals(7.5, r.value(ResultKey.WALL_B), 1e-9)
        assertEquals(35.0, r.value(ResultKey.WALL_TOTAL), 1e-9)
        assertEquals(30.0, r.value(ResultKey.ROOM_VOLUME), 1e-9)
    }

    @Test fun custom_depthIsOptional() {
        val flat = eval(MeasurementType.CUSTOM, mapOf(Field.WIDTH to "200", Field.HEIGHT to "100")).result!!
        assertEquals(2.0, flat.area!!, 1e-9)
        assertNull(flat.volume)
        val box = eval(MeasurementType.CUSTOM, mapOf(Field.WIDTH to "200", Field.HEIGHT to "100", Field.DEPTH to "50")).result!!
        assertEquals(1.0, box.volume!!, 1e-9)
    }

    @Test fun incompleteInput_isNotAnError() {
        val e = eval(MeasurementType.ROOM, mapOf(Field.LENGTH to "400", Field.WIDTH to ""))
        assertNull(e.result)
        assertNull(e.error)
    }

    @Test fun halfFilledOpeningRow_isIncompleteNotError() {
        val e = eval(
            MeasurementType.WALL,
            mapOf(Field.LENGTH to "500", Field.HEIGHT to "250"),
            doors = listOf("90" to "")
        )
        assertNull(e.result)
        assertNull(e.error)
        val blankRowIgnored = eval(
            MeasurementType.WALL,
            mapOf(Field.LENGTH to "500", Field.HEIGHT to "250"),
            doors = listOf("" to "")
        )
        assertNotNull(blankRowIgnored.result)
    }

    @Test fun invalidText_isReported() {
        assertEquals(MeasurementError.NOT_A_NUMBER,
            eval(MeasurementType.DOOR, mapOf(Field.WIDTH to "abc", Field.HEIGHT to "210")).error)
        assertEquals(MeasurementError.NEGATIVE_VALUE,
            eval(MeasurementType.DOOR, mapOf(Field.WIDTH to "-90", Field.HEIGHT to "210")).error)
        assertEquals(MeasurementError.ZERO_VALUE,
            eval(MeasurementType.DOOR, mapOf(Field.WIDTH to "0", Field.HEIGHT to "210")).error)
    }

    @Test fun persianDigitsWork() {
        val r = eval(MeasurementType.DOOR, mapOf(Field.WIDTH to "۹۰", Field.HEIGHT to "۲۱۰")).result!!
        assertEquals(1.89, r.area!!, 1e-9)
    }

    @Test fun requiredFieldsPerType() {
        assertTrue(ManualCalculations.supportsOpenings(MeasurementType.WALL))
        assertEquals(listOf(Field.WIDTH, Field.HEIGHT, Field.DEPTH), ManualCalculations.requiredFields(MeasurementType.CLOSET))
        assertEquals(listOf(Field.DEPTH), ManualCalculations.optionalFields(MeasurementType.CUSTOM))
    }
}
