package com.smartmeter3d.calculator

/** [lines] ve [error] ikisi de boşsa giriş henüz tamamlanmamıştır. */
data class CalcEvaluation(val lines: List<ResultLine>, val error: MeasurementError?)

object CalculatorEvaluator {
    private fun run(block: () -> List<ResultLine>?): CalcEvaluation =
        try {
            CalcEvaluation(block() ?: emptyList(), null)
        } catch (e: InvalidMeasurementException) {
            CalcEvaluation(emptyList(), e.error)
        }

    fun area(w: String, h: String, unit: LengthUnit): CalcEvaluation = run {
        if (w.isBlank() || h.isBlank()) null
        else listOf(
            ResultLine(
                ResultKey.AREA,
                MeasurementCalculator.rectangleArea(
                    InputParser.parseLengthToMeters(w, unit),
                    InputParser.parseLengthToMeters(h, unit)
                ),
                UnitKind.AREA
            )
        )
    }

    fun volume(l: String, w: String, h: String, unit: LengthUnit): CalcEvaluation = run {
        if (l.isBlank() || w.isBlank() || h.isBlank()) null
        else listOf(
            ResultLine(
                ResultKey.VOLUME,
                MeasurementCalculator.volume(
                    InputParser.parseLengthToMeters(l, unit),
                    InputParser.parseLengthToMeters(w, unit),
                    InputParser.parseLengthToMeters(h, unit)
                ),
                UnitKind.VOLUME
            )
        )
    }

    /** Girişler m² cinsindendir. Kapı/pencere boş bırakılırsa 0 sayılır. */
    fun netWall(wallText: String, doorText: String, windowText: String): CalcEvaluation = run {
        if (wallText.isBlank()) {
            null
        } else {
            val wall = InputParser.parseAreaToSquareMeters(wallText, AreaUnit.SQUARE_METER, allowZero = false)
            val door = if (doorText.isBlank()) 0.0
            else InputParser.parseAreaToSquareMeters(doorText, AreaUnit.SQUARE_METER, allowZero = true)
            val window = if (windowText.isBlank()) 0.0
            else InputParser.parseAreaToSquareMeters(windowText, AreaUnit.SQUARE_METER, allowZero = true)
            val net = MeasurementCalculator.netWallArea(wall, listOf(door), listOf(window))
            listOf(
                ResultLine(ResultKey.GROSS_AREA, wall, UnitKind.AREA),
                ResultLine(ResultKey.DOOR_AREA, door, UnitKind.AREA),
                ResultLine(ResultKey.WINDOW_AREA, window, UnitKind.AREA),
                ResultLine(ResultKey.NET_AREA, net, UnitKind.AREA)
            )
        }
    }

    /** Birim dönüştürme: sonuç hem m hem cm olarak gösterilir. */
    fun convertLength(text: String, unit: LengthUnit): CalcEvaluation = run {
        if (text.isBlank()) null
        else listOf(ResultLine(ResultKey.LENGTH, InputParser.parseLengthToMeters(text, unit), UnitKind.LENGTH))
    }

    fun convertArea(text: String, unit: AreaUnit): CalcEvaluation = run {
        if (text.isBlank()) null
        else listOf(
            ResultLine(ResultKey.AREA, InputParser.parseAreaToSquareMeters(text, unit, allowZero = false), UnitKind.AREA)
        )
    }
}
