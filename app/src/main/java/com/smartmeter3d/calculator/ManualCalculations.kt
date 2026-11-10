package com.smartmeter3d.calculator

import com.smartmeter3d.database.MeasurementType

enum class Field { LENGTH, WIDTH, HEIGHT, DEPTH }

enum class UnitKind { LENGTH, AREA, VOLUME, COUNT }

enum class ResultKey {
    LENGTH, WIDTH, HEIGHT, DEPTH,
    GROSS_AREA, DOOR_COUNT, DOOR_AREA, WINDOW_COUNT, WINDOW_AREA, NET_AREA,
    AREA, FRONT_AREA, FLOOR_AREA, WALL_A, WALL_B, WALL_TOTAL,
    VOLUME, VOLUME_APPROX, ROOM_VOLUME
}

data class ResultLine(val key: ResultKey, val value: Double, val kind: UnitKind)

/** Hesaplanan satırlar (gösterim için) ve Room'a kaydedilecek değerler (metre / m² / m³). */
data class ManualResult(
    val lines: List<ResultLine>,
    val length: Double? = null,
    val width: Double? = null,
    val height: Double? = null,
    val depth: Double? = null,
    val area: Double? = null,
    val volume: Double? = null
)

/** [result] ve [error] ikisi de null ise giriş henüz tamamlanmamıştır. */
data class ManualEvaluation(val result: ManualResult?, val error: MeasurementError?)

object ManualCalculations {
    fun requiredFields(type: MeasurementType): List<Field> = when (type) {
        MeasurementType.WALL -> listOf(Field.LENGTH, Field.HEIGHT)
        MeasurementType.DOOR, MeasurementType.WINDOW -> listOf(Field.WIDTH, Field.HEIGHT)
        MeasurementType.CLOSET -> listOf(Field.WIDTH, Field.HEIGHT, Field.DEPTH)
        MeasurementType.ROOM -> listOf(Field.LENGTH, Field.WIDTH, Field.HEIGHT)
        MeasurementType.CUSTOM -> listOf(Field.WIDTH, Field.HEIGHT)
    }

    fun optionalFields(type: MeasurementType): List<Field> =
        if (type == MeasurementType.CUSTOM) listOf(Field.DEPTH) else emptyList()

    fun supportsOpenings(type: MeasurementType): Boolean = type == MeasurementType.WALL

    /** Değerler metre cinsindendir. Geçersiz girişte InvalidMeasurementException fırlatır. */
    fun compute(
        type: MeasurementType,
        values: Map<Field, Double>,
        doors: List<Opening> = emptyList(),
        windows: List<Opening> = emptyList()
    ): ManualResult {
        fun req(f: Field): Double = values[f] ?: throw InvalidMeasurementException(MeasurementError.EMPTY_INPUT)

        return when (type) {
            MeasurementType.WALL -> {
                val length = req(Field.LENGTH)
                val height = req(Field.HEIGHT)
                val w = MeasurementCalculator.wall(length, height, doors, windows)
                ManualResult(
                    lines = listOf(
                        ResultLine(ResultKey.LENGTH, length, UnitKind.LENGTH),
                        ResultLine(ResultKey.HEIGHT, height, UnitKind.LENGTH),
                        ResultLine(ResultKey.GROSS_AREA, w.grossArea, UnitKind.AREA),
                        ResultLine(ResultKey.DOOR_COUNT, w.doorCount.toDouble(), UnitKind.COUNT),
                        ResultLine(ResultKey.DOOR_AREA, w.doorArea, UnitKind.AREA),
                        ResultLine(ResultKey.WINDOW_COUNT, w.windowCount.toDouble(), UnitKind.COUNT),
                        ResultLine(ResultKey.WINDOW_AREA, w.windowArea, UnitKind.AREA),
                        ResultLine(ResultKey.NET_AREA, w.netArea, UnitKind.AREA)
                    ),
                    length = length, height = height, area = w.netArea
                )
            }
            MeasurementType.DOOR, MeasurementType.WINDOW -> {
                val width = req(Field.WIDTH)
                val height = req(Field.HEIGHT)
                val area = MeasurementCalculator.rectangleArea(width, height)
                ManualResult(
                    lines = listOf(
                        ResultLine(ResultKey.WIDTH, width, UnitKind.LENGTH),
                        ResultLine(ResultKey.HEIGHT, height, UnitKind.LENGTH),
                        ResultLine(ResultKey.AREA, area, UnitKind.AREA)
                    ),
                    width = width, height = height, area = area
                )
            }
            MeasurementType.CLOSET -> {
                val width = req(Field.WIDTH)
                val height = req(Field.HEIGHT)
                val depth = req(Field.DEPTH)
                val r = MeasurementCalculator.closet(width, height, depth)
                ManualResult(
                    lines = listOf(
                        ResultLine(ResultKey.WIDTH, width, UnitKind.LENGTH),
                        ResultLine(ResultKey.HEIGHT, height, UnitKind.LENGTH),
                        ResultLine(ResultKey.DEPTH, depth, UnitKind.LENGTH),
                        ResultLine(ResultKey.FRONT_AREA, r.frontArea, UnitKind.AREA),
                        ResultLine(ResultKey.VOLUME_APPROX, r.volume, UnitKind.VOLUME)
                    ),
                    width = width, height = height, depth = depth, area = r.frontArea, volume = r.volume
                )
            }
            MeasurementType.ROOM -> {
                val length = req(Field.LENGTH)
                val width = req(Field.WIDTH)
                val height = req(Field.HEIGHT)
                val r = MeasurementCalculator.room(length, width, height)
                ManualResult(
                    lines = listOf(
                        ResultLine(ResultKey.LENGTH, length, UnitKind.LENGTH),
                        ResultLine(ResultKey.WIDTH, width, UnitKind.LENGTH),
                        ResultLine(ResultKey.HEIGHT, height, UnitKind.LENGTH),
                        ResultLine(ResultKey.FLOOR_AREA, r.floorArea, UnitKind.AREA),
                        ResultLine(ResultKey.WALL_A, r.wallAreas[0], UnitKind.AREA),
                        ResultLine(ResultKey.WALL_B, r.wallAreas[1], UnitKind.AREA),
                        ResultLine(ResultKey.WALL_TOTAL, r.totalWallArea, UnitKind.AREA),
                        ResultLine(ResultKey.ROOM_VOLUME, r.volume, UnitKind.VOLUME)
                    ),
                    length = length, width = width, height = height, area = r.floorArea, volume = r.volume
                )
            }
            MeasurementType.CUSTOM -> {
                val width = req(Field.WIDTH)
                val height = req(Field.HEIGHT)
                val depth = values[Field.DEPTH]
                val area = MeasurementCalculator.rectangleArea(width, height)
                val volume = depth?.let { MeasurementCalculator.volume(width, height, it) }
                val lines = ArrayList<ResultLine>()
                lines.add(ResultLine(ResultKey.WIDTH, width, UnitKind.LENGTH))
                lines.add(ResultLine(ResultKey.HEIGHT, height, UnitKind.LENGTH))
                if (depth != null) lines.add(ResultLine(ResultKey.DEPTH, depth, UnitKind.LENGTH))
                lines.add(ResultLine(ResultKey.AREA, area, UnitKind.AREA))
                if (volume != null) lines.add(ResultLine(ResultKey.VOLUME, volume, UnitKind.VOLUME))
                ManualResult(lines, width = width, height = height, depth = depth, area = area, volume = volume)
            }
        }
    }
}

/** Kullanıcının yazdığı metinleri ayrıştırıp hesaplar. Eksik alanlar hata değil, "tamamlanmamış" sayılır. */
object ManualEvaluator {
    fun evaluate(
        type: MeasurementType,
        texts: Map<Field, String>,
        unit: LengthUnit,
        doorTexts: List<Pair<String, String>> = emptyList(),
        windowTexts: List<Pair<String, String>> = emptyList()
    ): ManualEvaluation {
        return try {
            var incomplete = false
            val values = HashMap<Field, Double>()
            for (f in ManualCalculations.requiredFields(type)) {
                val t = texts[f].orEmpty()
                if (t.isBlank()) incomplete = true else values[f] = InputParser.parseLengthToMeters(t, unit)
            }
            for (f in ManualCalculations.optionalFields(type)) {
                val t = texts[f].orEmpty()
                if (t.isNotBlank()) values[f] = InputParser.parseLengthToMeters(t, unit)
            }
            val doors = parseOpenings(doorTexts, unit)
            val windows = parseOpenings(windowTexts, unit)
            if (incomplete || doors == null || windows == null) {
                ManualEvaluation(null, null)
            } else {
                ManualEvaluation(ManualCalculations.compute(type, values, doors, windows), null)
            }
        } catch (e: InvalidMeasurementException) {
            ManualEvaluation(null, e.error)
        }
    }

    /** Boş satırlar yok sayılır; yarım doldurulmuş satır varsa null (tamamlanmamış). */
    private fun parseOpenings(rows: List<Pair<String, String>>, unit: LengthUnit): List<Opening>? {
        val result = ArrayList<Opening>()
        var incomplete = false
        for ((w, h) in rows) {
            if (w.isBlank() && h.isBlank()) continue
            if (w.isBlank() || h.isBlank()) {
                incomplete = true
                continue
            }
            result.add(Opening(InputParser.parseLengthToMeters(w, unit), InputParser.parseLengthToMeters(h, unit)))
        }
        return if (incomplete) null else result
    }
}
