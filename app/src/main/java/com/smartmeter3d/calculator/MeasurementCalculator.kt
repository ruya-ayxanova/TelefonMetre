package com.smartmeter3d.calculator

data class Opening(val width: Double, val height: Double)

data class WallResult(
    val grossArea: Double,
    val doorArea: Double,
    val windowArea: Double,
    val netArea: Double,
    val doorCount: Int,
    val windowCount: Int
)

data class ClosetResult(val frontArea: Double, val volume: Double)

data class RoomResult(
    val floorArea: Double,
    val wallAreas: List<Double>,
    val totalWallArea: Double,
    val volume: Double
)

/** Tüm giriş ve çıkışlar metre / metrekare / metreküp cinsindendir. Double kullanılır. */
object MeasurementCalculator {
    const val MAX_DIMENSION_METERS = 1000.0

    fun validateDimension(value: Double): Double {
        if (value.isNaN()) throw InvalidMeasurementException(MeasurementError.NOT_A_NUMBER)
        if (value.isInfinite()) throw InvalidMeasurementException(MeasurementError.NOT_FINITE)
        if (value < 0.0) throw InvalidMeasurementException(MeasurementError.NEGATIVE_VALUE)
        if (value == 0.0) throw InvalidMeasurementException(MeasurementError.ZERO_VALUE)
        if (value > MAX_DIMENSION_METERS) throw InvalidMeasurementException(MeasurementError.TOO_LARGE)
        return value
    }

    /** Alan = Genişlik × Yükseklik */
    fun rectangleArea(width: Double, height: Double): Double =
        validateDimension(width) * validateDimension(height)

    /** Hacim = Uzunluk × Genişlik × Yükseklik */
    fun volume(length: Double, width: Double, height: Double): Double =
        validateDimension(length) * validateDimension(width) * validateDimension(height)

    /** Net Alan = Duvar Alanı − Kapı Alanları − Pencere Alanları */
    fun netWallArea(wallArea: Double, doorAreas: List<Double>, windowAreas: List<Double>): Double {
        if (wallArea.isNaN() || wallArea.isInfinite()) throw InvalidMeasurementException(MeasurementError.NOT_FINITE)
        if (wallArea <= 0.0) throw InvalidMeasurementException(MeasurementError.ZERO_VALUE)
        (doorAreas + windowAreas).forEach {
            if (it.isNaN() || it.isInfinite()) throw InvalidMeasurementException(MeasurementError.NOT_FINITE)
            if (it < 0.0) throw InvalidMeasurementException(MeasurementError.NEGATIVE_VALUE)
        }
        val net = wallArea - doorAreas.sum() - windowAreas.sum()
        if (net < 0.0) throw InvalidMeasurementException(MeasurementError.OPENINGS_EXCEED_WALL)
        return net
    }

    fun wall(
        length: Double,
        height: Double,
        doors: List<Opening> = emptyList(),
        windows: List<Opening> = emptyList()
    ): WallResult {
        val gross = rectangleArea(length, height)
        val doorAreas = doors.map { rectangleArea(it.width, it.height) }
        val windowAreas = windows.map { rectangleArea(it.width, it.height) }
        val net = netWallArea(gross, doorAreas, windowAreas)
        return WallResult(gross, doorAreas.sum(), windowAreas.sum(), net, doors.size, windows.size)
    }

    fun closet(width: Double, height: Double, depth: Double): ClosetResult =
        ClosetResult(frontArea = rectangleArea(width, height), volume = volume(width, depth, height))

    fun room(length: Double, width: Double, height: Double): RoomResult {
        val floor = rectangleArea(length, width)
        val lengthWall = rectangleArea(length, height)
        val widthWall = rectangleArea(width, height)
        val walls = listOf(lengthWall, widthWall, lengthWall, widthWall)
        return RoomResult(floor, walls, walls.sum(), volume(length, width, height))
    }
}
