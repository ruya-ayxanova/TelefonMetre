package com.smartmeter3d.measurement

import com.smartmeter3d.calculator.InvalidMeasurementException
import com.smartmeter3d.calculator.MeasurementCalculator
import com.smartmeter3d.calculator.MeasurementError
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

data class Point2(val x: Double, val y: Double)

object PhotoMath {
    const val MIN_REFERENCE_PIXELS = 5.0
    const val MIN_TARGET_PIXELS = 2.0

    fun pixelDistance(a: Point2, b: Point2): Double = hypot(a.x - b.x, a.y - b.y)

    /** Gerçek referans uzunluğu olmadan ölçüm yapılamaz. */
    fun requireReferenceLength(meters: Double): Double {
        if (meters.isNaN() || meters <= 0.0) throw InvalidMeasurementException(MeasurementError.REFERENCE_REQUIRED)
        return MeasurementCalculator.validateDimension(meters)
    }

    /** Referans doğru parçasına göre metre/piksel ölçeği. Yalnızca referansla aynı düzlemdeki mesafeler için geçerlidir. */
    fun metersPerPixel(refA: Point2, refB: Point2, refMeters: Double): Double {
        val length = requireReferenceLength(refMeters)
        val px = pixelDistance(refA, refB)
        if (px < MIN_REFERENCE_PIXELS) throw InvalidMeasurementException(MeasurementError.POINTS_TOO_CLOSE)
        return length / px
    }

    fun measureDistance(refA: Point2, refB: Point2, refMeters: Double, a: Point2, b: Point2): Double {
        val scale = metersPerPixel(refA, refB, refMeters)
        val px = pixelDistance(a, b)
        if (px < MIN_TARGET_PIXELS) throw InvalidMeasurementException(MeasurementError.POINTS_TOO_CLOSE)
        return px * scale
    }
}

object Quad {
    /** 4 köşe sırayla seçilmiş, dışbükey, çapraz olmayan ve doğrusal olmayan bir dörtgen mi? */
    fun isValid(p: List<Point2>): Boolean {
        if (p.size != 4) return false
        var sign = 0
        for (i in 0 until 4) {
            val a = p[i]
            val b = p[(i + 1) % 4]
            val c = p[(i + 2) % 4]
            val z = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            if (abs(z) < 1e-3) return false
            val s = if (z > 0) 1 else -1
            if (sign == 0) sign = s else if (s != sign) return false
        }
        return true
    }

    fun area(p: List<Point2>): Double {
        var sum = 0.0
        for (i in p.indices) {
            val a = p[i]
            val b = p[(i + 1) % p.size]
            sum += a.x * b.y - b.x * a.y
        }
        return abs(sum) / 2.0
    }
}

/** 4 noktalık eşleşmeden perspektif dönüşümü (homography). Koordinatlar sayısal kararlılık için normalize edilir. */
class Homography private constructor(
    private val h: DoubleArray,
    private val srcCenter: Point2,
    private val srcScale: Double,
    private val dstCenter: Point2,
    private val dstScale: Double
) {
    fun denominator(p: Point2): Double {
        val x = (p.x - srcCenter.x) / srcScale
        val y = (p.y - srcCenter.y) / srcScale
        return h[6] * x + h[7] * y + h[8]
    }

    fun map(p: Point2): Point2? {
        val x = (p.x - srcCenter.x) / srcScale
        val y = (p.y - srcCenter.y) / srcScale
        val w = h[6] * x + h[7] * y + h[8]
        if (abs(w) < 1e-12) return null
        val u = (h[0] * x + h[1] * y + h[2]) / w
        val v = (h[3] * x + h[4] * y + h[5]) / w
        return Point2(u * dstScale + dstCenter.x, v * dstScale + dstCenter.y)
    }

    companion object {
        fun fromCorrespondences(src: List<Point2>, dst: List<Point2>): Homography? {
            if (src.size != 4 || dst.size != 4) return null
            val (sc, ss) = normalization(src)
            val (dc, ds) = normalization(dst)
            if (ss < 1e-9 || ds < 1e-9) return null

            val a = Array(8) { DoubleArray(9) }
            for (i in 0 until 4) {
                val x = (src[i].x - sc.x) / ss
                val y = (src[i].y - sc.y) / ss
                val u = (dst[i].x - dc.x) / ds
                val v = (dst[i].y - dc.y) / ds
                a[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -u * x, -u * y, u)
                a[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -v * x, -v * y, v)
            }
            val sol = solve(a) ?: return null
            val h = DoubleArray(9)
            for (i in 0 until 8) h[i] = sol[i]
            h[8] = 1.0
            return Homography(h, sc, ss, dc, ds)
        }

        private fun normalization(pts: List<Point2>): Pair<Point2, Double> {
            val cx = pts.sumOf { it.x } / pts.size
            val cy = pts.sumOf { it.y } / pts.size
            val s = pts.sumOf { hypot(it.x - cx, it.y - cy) } / pts.size
            return Pair(Point2(cx, cy), s)
        }

        /** Kısmi pivotlu Gauss-Jordan; 8x9 genişletilmiş matris. Tekilse null. */
        private fun solve(a: Array<DoubleArray>): DoubleArray? {
            val n = a.size
            for (c in 0 until n) {
                var p = c
                for (r in c + 1 until n) if (abs(a[r][c]) > abs(a[p][c])) p = r
                if (abs(a[p][c]) < 1e-12) return null
                val tmp = a[c]; a[c] = a[p]; a[p] = tmp
                for (r in 0 until n) {
                    if (r == c) continue
                    val f = a[r][c] / a[c][c]
                    for (k in c..n) a[r][k] -= f * a[c][k]
                }
            }
            return DoubleArray(n) { a[it][n] / a[it][it] }
        }
    }
}

data class RectangleResult(
    val width: Double,
    val height: Double,
    val area: Double,
    /** Karşılıklı kenarların birbirinden sapması (%). Büyükse köşeler veya yüzey dikdörtgen değildir. */
    val sideMismatchPercent: Double
)

object RectangleMeasurer {
    /**
     * [refCorners]: boyutları bilinen dikdörtgenin görüntüdeki 4 köşesi (sol üst, sağ üst, sağ alt, sol alt).
     * [targetCorners]: ölçülecek dikdörtgenin köşeleri, aynı sırayla ve aynı düzlemde.
     */
    fun measure(
        refCorners: List<Point2>,
        refWidth: Double,
        refHeight: Double,
        targetCorners: List<Point2>
    ): RectangleResult {
        val rw = PhotoMath.requireReferenceLength(refWidth)
        val rh = PhotoMath.requireReferenceLength(refHeight)
        if (!Quad.isValid(refCorners) || !Quad.isValid(targetCorners)) {
            throw InvalidMeasurementException(MeasurementError.DEGENERATE_POINTS)
        }
        val metricRef = listOf(Point2(0.0, 0.0), Point2(rw, 0.0), Point2(rw, rh), Point2(0.0, rh))
        val hom = Homography.fromCorrespondences(refCorners, metricRef)
            ?: throw InvalidMeasurementException(MeasurementError.DEGENERATE_POINTS)

        val refSign = if (hom.denominator(refCorners[0]) > 0) 1.0 else -1.0
        if (refCorners.any { hom.denominator(it) * refSign <= 0.0 }) {
            throw InvalidMeasurementException(MeasurementError.DEGENERATE_POINTS)
        }
        val mapped = targetCorners.map { p ->
            if (hom.denominator(p) * refSign <= 0.0) {
                throw InvalidMeasurementException(MeasurementError.DEGENERATE_POINTS)
            }
            hom.map(p) ?: throw InvalidMeasurementException(MeasurementError.DEGENERATE_POINTS)
        }

        val w1 = PhotoMath.pixelDistance(mapped[0], mapped[1])
        val w2 = PhotoMath.pixelDistance(mapped[3], mapped[2])
        val h1 = PhotoMath.pixelDistance(mapped[0], mapped[3])
        val h2 = PhotoMath.pixelDistance(mapped[1], mapped[2])
        val width = (w1 + w2) / 2.0
        val height = (h1 + h2) / 2.0
        if (width < 1e-6 || height < 1e-6) throw InvalidMeasurementException(MeasurementError.DEGENERATE_POINTS)

        val mismatch = max(abs(w1 - w2) / width, abs(h1 - h2) / height) * 100.0
        return RectangleResult(width, height, Quad.area(mapped), mismatch)
    }
}
