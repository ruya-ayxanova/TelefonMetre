package com.smartmeter3d.ui

import androidx.annotation.StringRes
import com.smartmeter3d.R
import com.smartmeter3d.calculator.Field
import com.smartmeter3d.calculator.ResultKey
import com.smartmeter3d.database.MeasurementMethod
import com.smartmeter3d.database.MeasurementType

@StringRes
fun ResultKey.labelRes(): Int = when (this) {
    ResultKey.LENGTH -> R.string.field_length
    ResultKey.WIDTH -> R.string.field_width
    ResultKey.HEIGHT -> R.string.field_height
    ResultKey.DEPTH -> R.string.field_depth
    ResultKey.GROSS_AREA -> R.string.res_gross_area
    ResultKey.DOOR_COUNT -> R.string.res_door_count
    ResultKey.DOOR_AREA -> R.string.res_door_area
    ResultKey.WINDOW_COUNT -> R.string.res_window_count
    ResultKey.WINDOW_AREA -> R.string.res_window_area
    ResultKey.NET_AREA -> R.string.res_net_area
    ResultKey.AREA -> R.string.field_area
    ResultKey.FRONT_AREA -> R.string.res_front_area
    ResultKey.FLOOR_AREA -> R.string.res_floor_area
    ResultKey.WALL_A -> R.string.res_wall_a
    ResultKey.WALL_B -> R.string.res_wall_b
    ResultKey.WALL_TOTAL -> R.string.res_wall_total
    ResultKey.VOLUME -> R.string.field_volume
    ResultKey.VOLUME_APPROX -> R.string.res_volume_approx
    ResultKey.ROOM_VOLUME -> R.string.res_room_volume
}

@StringRes
fun Field.labelRes(optional: Boolean = false): Int = when (this) {
    Field.LENGTH -> R.string.field_length
    Field.WIDTH -> R.string.field_width
    Field.HEIGHT -> R.string.field_height
    Field.DEPTH -> if (optional) R.string.field_depth_optional else R.string.field_depth
}

@StringRes
fun MeasurementType.titleRes(): Int = when (this) {
    MeasurementType.WALL -> R.string.menu_wall
    MeasurementType.DOOR -> R.string.menu_door
    MeasurementType.WINDOW -> R.string.menu_window
    MeasurementType.CLOSET -> R.string.menu_closet
    MeasurementType.ROOM -> R.string.menu_room
    MeasurementType.CUSTOM -> R.string.menu_custom
}

@StringRes
fun MeasurementMethod.methodLabelRes(): Int = when (this) {
    MeasurementMethod.AR -> R.string.method_ar
    MeasurementMethod.PHOTO -> R.string.method_photo
    MeasurementMethod.MANUAL -> R.string.method_manual
}
