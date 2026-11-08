package com.smartmeter3d.calculator

enum class MeasurementError {
    EMPTY_INPUT,
    NOT_A_NUMBER,
    NOT_FINITE,
    NEGATIVE_VALUE,
    ZERO_VALUE,
    TOO_LARGE,
    OPENINGS_EXCEED_WALL
}

class InvalidMeasurementException(val error: MeasurementError) :
    IllegalArgumentException(error.name)
