package com.smartmeter3d.ar

import com.smartmeter3d.measurement.Vec3

enum class ArIssue { NONE, INSUFFICIENT_LIGHT, EXCESSIVE_MOTION, INSUFFICIENT_FEATURES, CAMERA_UNAVAILABLE, BAD_STATE }

enum class SurfaceKind { NONE, WALL, FLOOR, CEILING }

enum class ArSetupResult {
    READY, INSTALL_REQUESTED, NOT_SUPPORTED, NOT_INSTALLED, APK_TOO_OLD, SDK_TOO_OLD, CAMERA_ERROR, ERROR
}

data class ArPointInfo(
    val world: Vec3,
    val startsNewChain: Boolean,
    val screenX: Float,
    val screenY: Float,
    val onScreen: Boolean
)

data class ArFrameState(
    val tracking: Boolean = false,
    val issue: ArIssue = ArIssue.NONE,
    val planeCount: Int = 0,
    val wallCount: Int = 0,
    val crosshairSurface: SurfaceKind = SurfaceKind.NONE,
    val lowLight: Boolean = false,
    val points: List<ArPointInfo> = emptyList()
) {
    /** Ölçüm noktası yalnızca takip sağlıklıyken ve artı işareti algılanmış bir yüzeyin üzerindeyken eklenebilir. */
    val canPlace: Boolean get() = tracking && crosshairSurface != SurfaceKind.NONE
}

sealed interface ArCommand {
    data class Place(val startsNewChain: Boolean) : ArCommand
    object Undo : ArCommand
    object Clear : ArCommand
}
