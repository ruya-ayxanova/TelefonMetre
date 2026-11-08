package com.smartmeter3d.measurement

import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.MeasurementError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGeometryTest {
    private val delta = 1e-6

    private fun errorOf(block: () -> Unit): MeasurementError =
        assertThrows(InvalidMeasurementException::class.java) { block() }.error

    /** Gerçek (bilinen) perspektif: metre düzlemi -> görüntü pikseli. */
    private fun project(x: Double, y: Double): Point2 {
        val w = 0.0004 * x + 0.0002 * y + 1.0
        return Point2((800 * x + 50 * y + 300) / w, (30 * x + 780 * y + 200) / w)
    }

    private fun metric(vararg xy: Pair<Double, Double>) = xy.map { project(it.first, it.second) }

    private val refMetric = arrayOf(0.0 to 0.0, 0.9 to 0.0, 0.9 to 2.1, 0.0 to 2.1)

    @Test fun distance_usesReferenceScale() {
        val m = PhotoMath.measureDistance(
            Point2(0.0, 0.0), Point2(100.0, 0.0), 0.5,
            Point2(10.0, 10.0), Point2(310.0, 10.0)
        )
        assertEquals(1.5, m, delta)
    }

    @Test fun distance_requiresRealReference() {
        assertEquals(MeasurementError.REFERENCE_REQUIRED, errorOf {
            PhotoMath.measureDistance(Point2(0.0, 0.0), Point2(100.0, 0.0), 0.0, Point2(0.0, 0.0), Point2(50.0, 0.0))
        })
        assertEquals(MeasurementError.REFERENCE_REQUIRED, errorOf {
            PhotoMath.measureDistance(Point2(0.0, 0.0), Point2(100.0, 0.0), Double.NaN, Point2(0.0, 0.0), Point2(50.0, 0.0))
        })
    }

    @Test fun distance_rejectsPointsTooClose() {
        assertEquals(MeasurementError.POINTS_TOO_CLOSE, errorOf {
            PhotoMath.measureDistance(Point2(0.0, 0.0), Point2(2.0, 0.0), 0.5, Point2(0.0, 0.0), Point2(50.0, 0.0))
        })
        assertEquals(MeasurementError.POINTS_TOO_CLOSE, errorOf {
            PhotoMath.measureDistance(Point2(0.0, 0.0), Point2(100.0, 0.0), 0.5, Point2(5.0, 5.0), Point2(5.5, 5.0))
        })
    }

    @Test fun homography_mapsCorrespondencesExactly() {
        val src = metric(*refMetric)
        val dst = refMetric.map { Point2(it.first, it.second) }
        val h = Homography.fromCorrespondences(src, dst)
        assertNotNull(h)
        for (i in 0 until 4) {
            val p = h!!.map(src[i])!!
            assertEquals(dst[i].x, p.x, delta)
            assertEquals(dst[i].y, p.y, delta)
        }
    }

    @Test fun perspectiveRectangle_recoversTrueSize() {
        val r = RectangleMeasurer.measure(
            refCorners = metric(*refMetric),
            refWidth = 0.9,
            refHeight = 2.1,
            targetCorners = metric(1.0 to 0.5, 2.2 to 0.5, 2.2 to 1.5, 1.0 to 1.5)
        )
        assertEquals(1.2, r.width, delta)
        assertEquals(1.0, r.height, delta)
        assertEquals(1.2, r.area, delta)
        assertTrue(r.sideMismatchPercent < 1e-4)
    }

    @Test fun frontalRectangle_usesPlainScale() {
        val ref = listOf(Point2(100.0, 100.0), Point2(280.0, 100.0), Point2(280.0, 520.0), Point2(100.0, 520.0))
        val target = listOf(Point2(300.0, 200.0), Point2(540.0, 200.0), Point2(540.0, 400.0), Point2(300.0, 400.0))
        val r = RectangleMeasurer.measure(ref, 0.9, 2.1, target)
        assertEquals(1.2, r.width, delta)
        assertEquals(1.0, r.height, delta)
        assertEquals(1.2, r.area, delta)
    }

    @Test fun nonRectangularTarget_reportsMismatch() {
        val r = RectangleMeasurer.measure(
            metric(*refMetric), 0.9, 2.1,
            metric(1.0 to 0.5, 3.0 to 0.5, 2.5 to 1.5, 1.0 to 1.5)
        )
        assertTrue(r.sideMismatchPercent > 20.0)
    }

    @Test fun bowTieCorners_areRejected() {
        val ref = metric(*refMetric)
        val sq = listOf(Point2(300.0, 200.0), Point2(540.0, 200.0), Point2(540.0, 400.0), Point2(300.0, 400.0))
        val bowTie = listOf(sq[0], sq[2], sq[1], sq[3])
        assertEquals(MeasurementError.DEGENERATE_POINTS, errorOf {
            RectangleMeasurer.measure(ref, 0.9, 2.1, bowTie)
        })
    }

    @Test fun collinearReference_isRejected() {
        val collinear = listOf(Point2(0.0, 0.0), Point2(100.0, 0.0), Point2(200.0, 0.0), Point2(300.0, 0.0))
        val sq = listOf(Point2(300.0, 200.0), Point2(540.0, 200.0), Point2(540.0, 400.0), Point2(300.0, 400.0))
        assertEquals(MeasurementError.DEGENERATE_POINTS, errorOf {
            RectangleMeasurer.measure(collinear, 0.9, 2.1, sq)
        })
    }

    @Test fun missingReferenceDimensions_areRejected() {
        val ref = metric(*refMetric)
        val target = metric(1.0 to 0.5, 2.2 to 0.5, 2.2 to 1.5, 1.0 to 1.5)
        assertEquals(MeasurementError.REFERENCE_REQUIRED, errorOf { RectangleMeasurer.measure(ref, 0.0, 2.1, target) })
        assertEquals(MeasurementError.REFERENCE_REQUIRED, errorOf { RectangleMeasurer.measure(ref, 0.9, -1.0, target) })
    }

    @Test fun quadArea_isShoelace() {
        val q = listOf(Point2(0.0, 0.0), Point2(4.0, 0.0), Point2(4.0, 3.0), Point2(0.0, 3.0))
        assertEquals(12.0, Quad.area(q), delta)
    }
}
