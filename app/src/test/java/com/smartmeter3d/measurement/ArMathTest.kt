package com.smartmeter3d.measurement

import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.MeasurementError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ArMathTest {
    private val delta = 1e-9

    private fun v(x: Double, y: Double, z: Double = 0.0) = Vec3(x, y, z)
    private fun p(x: Double, y: Double, newChain: Boolean = false) = ArPointData(v(x, y), newChain)

    private fun errorOf(block: () -> Unit): MeasurementError =
        assertThrows(InvalidMeasurementException::class.java) { block() }.error

    @Test fun distance_is3dEuclidean() {
        assertEquals(5.0, ArMath.distance(v(0.0, 0.0, 0.0), v(3.0, 4.0, 0.0)), delta)
        assertEquals(3.0, ArMath.distance(v(1.0, 2.0, 3.0), v(3.0, 4.0, 4.0)), delta)
    }

    @Test fun angle_atCorner() {
        assertEquals(90.0, ArMath.angleDegrees(v(1.0, 0.0), v(0.0, 0.0), v(0.0, 1.0))!!, 1e-6)
        assertEquals(180.0, ArMath.angleDegrees(v(-1.0, 0.0), v(0.0, 0.0), v(1.0, 0.0))!!, 1e-6)
        assertNull(ArMath.angleDegrees(v(0.0, 0.0), v(0.0, 0.0), v(1.0, 0.0)))
    }

    @Test fun rectangle_fromThreeCorners() {
        val r = ArMath.rectangleFromThree(v(0.0, 0.0), v(2.0, 0.0), v(2.0, 1.5))
        assertEquals(2.0, r.width, delta)
        assertEquals(1.5, r.height, delta)
        assertEquals(3.0, r.area, delta)
        assertEquals(90.0, r.cornerAngleDegrees, 1e-6)
    }

    @Test fun rectangle_reportsNonRightAngle() {
        val r = ArMath.rectangleFromThree(v(0.0, 0.0), v(2.0, 0.0), v(3.0, 1.0))
        assertTrue(kotlin.math.abs(r.cornerAngleDegrees - 90.0) > 10.0)
    }

    @Test fun rectangle_rejectsCollinearAndTooClosePoints() {
        assertEquals(MeasurementError.DEGENERATE_POINTS, errorOf {
            ArMath.rectangleFromThree(v(0.0, 0.0), v(1.0, 0.0), v(2.0, 0.0))
        })
        assertEquals(MeasurementError.POINTS_TOO_CLOSE, errorOf {
            ArMath.rectangleFromThree(v(0.0, 0.0), v(0.001, 0.0), v(0.001, 1.0))
        })
    }

    @Test fun evaluate_distanceWithTwoChains() {
        val points = listOf(
            p(0.0, 0.0, true), p(1.0, 0.0), p(1.0, 1.0),   // zincir 1: kenarlar 1 + 1
            p(5.0, 5.0, true), p(7.0, 5.0)                  // zincir 2: kenar 2
        )
        val e = ArMath.evaluate(ArMode.DISTANCE, points)
        val outcome = e.outcome as ArOutcome.Distance
        assertEquals(listOf(1.0, 1.0, 2.0), outcome.edges)
        assertEquals(4.0, outcome.total, delta)
        assertNull(e.error)
    }

    @Test fun evaluate_singlePointHasNoOutcome() {
        val e = ArMath.evaluate(ArMode.DISTANCE, listOf(p(0.0, 0.0, true)))
        assertNull(e.outcome)
        assertNull(e.error)
    }

    @Test fun evaluate_tooCloseEdgeIsReportedNotHidden() {
        val e = ArMath.evaluate(ArMode.DISTANCE, listOf(p(0.0, 0.0, true), p(0.001, 0.0)))
        assertNull(e.outcome)
        assertEquals(MeasurementError.POINTS_TOO_CLOSE, e.error)
    }

    @Test fun evaluate_rectangleNeedsExactlyThreePoints() {
        assertNull(ArMath.evaluate(ArMode.RECTANGLE, listOf(p(0.0, 0.0), p(1.0, 0.0))).outcome)
        val e = ArMath.evaluate(ArMode.RECTANGLE, listOf(p(0.0, 0.0), p(2.0, 0.0), p(2.0, 1.0)))
        assertNotNull(e.outcome)
        assertEquals(2.0, (e.outcome as ArOutcome.Rect).result.area, delta)
    }

    @Test fun compare_arWithManual() {
        val c = ArMath.compare(1.02, 1.00)
        assertEquals(0.02, c.differenceMeters, 1e-9)
        assertEquals(2.0, c.percent, 1e-9)
        val negative = ArMath.compare(0.95, 1.00)
        assertEquals(-5.0, negative.percent, 1e-9)
    }

    @Test fun compare_rejectsInvalidManualValue() {
        assertEquals(MeasurementError.ZERO_VALUE, errorOf { ArMath.compare(1.0, 0.0) })
        assertEquals(MeasurementError.NEGATIVE_VALUE, errorOf { ArMath.compare(1.0, -2.0) })
    }
}
