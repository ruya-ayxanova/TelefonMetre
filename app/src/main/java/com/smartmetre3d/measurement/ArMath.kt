package com.smartmeter3d.measurement

import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.MeasurementCalculator
import com.smartmeter3d.calculator.MeasurementError
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

data class Vec3(val x: Double, val y: Double, val z: Double)

/** AR'de yerleştirilen bir nokta. [startsNewChain]: bu nokta önceki noktaya bağlanmaz, yeni bir kenar zincirini başlatır. */
data class ArPointData(val world: Vec3, val startsNewChain: Boolean)

enum class ArMode { DISTANCE, RECTANGLE }

data class ArRectResult(val width: Double, val height: Double, val area: Double, val cornerAngleDegrees: Double)

data class ArComparison(
    val arMeters: Double,
    val manualMeters: Double,
    /** AR − manuel (metre). */
    val differenceMeters: Double,
    /** Fark, manuel değere göre yüzde. */
    val percent: Double
)

sealed interface ArOutcome {
    data class Distance(val edges: List<Double>, val total: Double) : ArOutcome
    data class Rect(val result: ArRectResult) : ArOutcome
}

data class ArEvaluation(val outcome: ArOutcome?, val error: MeasurementError?)

object ArMath {
    const val MIN_EDGE_METERS = 0.005
    const val MIN_CORNER_ANGLE = 15.0
    const val MAX_CORNER_ANGLE = 165.0

    fun distance(a: Vec3, b: Vec3): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /** [b] köşesindeki açı (derece), 0..180. Noktalardan biri [b] ile çakışıksa null. */
    fun angleDegrees(a: Vec3, b: Vec3, c: Vec3): Double? {
        val v1 = Vec3(a.x - b.x, a.y - b.y, a.z - b.z)
        val v2 = Vec3(c.x - b.x, c.y - b.y, c.z - b.z)
        val n1 = sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z)
        val n2 = sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z)
        if (n1 < 1e-12 || n2 < 1e-12) return null
        val cos = ((v1.x * v2.x + v1.y * v2.y + v1.z * v2.z) / (n1 * n2)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos))
    }

    /** Dikdörtgenin 3 köşesinden (sol alt, sağ alt, sağ üst) genişlik, yükseklik ve alan. */
    fun rectangleFromThree(p0: Vec3, p1: Vec3, p2: Vec3): ArRectResult {
        val width = distance(p0, p1)
        val height = distance(p1, p2)
        if (width < MIN_EDGE_METERS || height < MIN_EDGE_METERS) {
            throw InvalidMeasurementException(MeasurementError.POINTS_TOO_CLOSE)
        }
        val angle = angleDegrees(p0, p1, p2)
            ?: throw InvalidMeasurementException(MeasurementError.POINTS_TOO_CLOSE)
        if (angle < MIN_CORNER_ANGLE || angle > MAX_CORNER_ANGLE) {
            throw InvalidMeasurementException(MeasurementError.DEGENERATE_POINTS)
        }
        val area = MeasurementCalculator.rectangleArea(width, height)
        return ArRectResult(width, height, area, angle)
    }

    fun compare(arMeters: Double, manualMeters: Double): ArComparison {
        if (arMeters.isNaN() || arMeters.isInfinite() || arMeters <= 0.0) {
            throw InvalidMeasurementException(MeasurementError.NOT_FINITE)
        }
        val manual = MeasurementCalculator.validateDimension(manualMeters)
        val diff = arMeters - manual
        return ArComparison(arMeters, manual, diff, diff / manual * 100.0)
    }

    fun evaluate(mode: ArMode, points: List<ArPointData>): ArEvaluation =
        try {
            ArEvaluation(
                when (mode) {
                    ArMode.DISTANCE -> distanceOutcome(points)
                    ArMode.RECTANGLE -> rectOutcome(points)
                },
                null
            )
        } catch (e: InvalidMeasurementException) {
            ArEvaluation(null, e.error)
        }

    private fun distanceOutcome(points: List<ArPointData>): ArOutcome? {
        val edges = ArrayList<Double>()
        for (i in 1 until points.size) {
            if (points[i].startsNewChain) continue
            val d = distance(points[i - 1].world, points[i].world)
            if (d < MIN_EDGE_METERS) throw InvalidMeasurementException(MeasurementError.POINTS_TOO_CLOSE)
            edges.add(d)
        }
        if (edges.isEmpty()) return null
        return ArOutcome.Distance(edges, edges.sum())
    }

    private fun rectOutcome(points: List<ArPointData>): ArOutcome? {
        if (points.size != 3) return null
        return ArOutcome.Rect(rectangleFromThree(points[0].world, points[1].world, points[2].world))
    }

    /** İki değer yüzde olarak ne kadar yakın? (test/yardımcı) */
    fun relativeDifference(a: Double, b: Double): Double = abs(a - b) / b
}
