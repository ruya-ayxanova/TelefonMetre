package com.smartmeter3d.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.LightEstimate
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.smartmeter3d.measurement.Vec3
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs

/** GL iş parçacığında çalışır: kareyi günceller, arka planı çizer, noktaları (Anchor) yönetir ve durumu yayınlar. */
class ArRenderer(private val controller: ArController) : GLSurfaceView.Renderer {

    private class Placed(val anchor: Anchor, val startsNewChain: Boolean)

    @Volatile var listener: ((ArFrameState) -> Unit)? = null

    private val background = BackgroundRenderer()
    private val commands = ConcurrentLinkedQueue<ArCommand>()
    private val placed = ArrayList<Placed>()

    private var viewportW = 0
    private var viewportH = 0
    private var viewportChanged = false
    private var lastRotation = -1
    private var boundSession: Session? = null

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val world = FloatArray(4)
    private val clip = FloatArray(4)

    fun enqueue(command: ArCommand) {
        commands.add(command)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        background.createOnGlThread()
        boundSession = null
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportW = width
        viewportH = height
        viewportChanged = true
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val session = controller.session ?: return
        try {
            if (session !== boundSession) {
                session.setCameraTextureName(background.textureId)
                boundSession = session
                placed.clear() // eski oturumun anchor'ları geçersiz
                viewportChanged = true
            }
            val rotation = controller.displayRotation()
            if (viewportChanged || rotation != lastRotation) {
                session.setDisplayGeometry(rotation, viewportW, viewportH)
                viewportChanged = false
                lastRotation = rotation
            }
            val frame = session.update()
            background.draw(frame)
            val camera = frame.camera
            val tracking = camera.trackingState == TrackingState.TRACKING
            processCommands(frame, tracking)
            publish(frame, camera, session, tracking)
        } catch (e: CameraNotAvailableException) {
            listener?.invoke(ArFrameState(issue = ArIssue.CAMERA_UNAVAILABLE))
        } catch (e: Exception) {
            // Oturum duraklatılmış veya kapanmış olabilir; sonraki karede yeniden denenir.
        }
    }

    private fun centerHit(frame: Frame, tracking: Boolean): HitResult? {
        if (!tracking || viewportW == 0) return null
        for (hit in frame.hitTest(viewportW / 2f, viewportH / 2f)) {
            val trackable = hit.trackable
            if (trackable is Plane &&
                trackable.trackingState == TrackingState.TRACKING &&
                trackable.isPoseInPolygon(hit.hitPose)
            ) return hit
        }
        return null
    }

    private fun processCommands(frame: Frame, tracking: Boolean) {
        while (true) {
            val command = commands.poll() ?: break
            when (command) {
                is ArCommand.Place -> {
                    val hit = centerHit(frame, tracking)
                    if (hit != null) placed.add(Placed(hit.createAnchor(), command.startsNewChain))
                }
                ArCommand.Undo -> {
                    if (placed.isNotEmpty()) detachQuietly(placed.removeAt(placed.lastIndex).anchor)
                }
                ArCommand.Clear -> {
                    placed.forEach { detachQuietly(it.anchor) }
                    placed.clear()
                }
            }
        }
    }

    private fun detachQuietly(anchor: Anchor) {
        try {
            anchor.detach()
        } catch (e: Exception) {
            // oturum kapanmış olabilir
        }
    }

    private fun publish(frame: Frame, camera: Camera, session: Session, tracking: Boolean) {
        val planes = session.getAllTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }
        val wallCount = planes.count { it.type == Plane.Type.VERTICAL }

        val hit = centerHit(frame, tracking)
        val surface = (hit?.trackable as? Plane)?.let {
            when (it.type) {
                Plane.Type.VERTICAL -> SurfaceKind.WALL
                Plane.Type.HORIZONTAL_UPWARD_FACING -> SurfaceKind.FLOOR
                Plane.Type.HORIZONTAL_DOWNWARD_FACING -> SurfaceKind.CEILING
                else -> SurfaceKind.NONE
            }
        } ?: SurfaceKind.NONE

        val issue = if (tracking) ArIssue.NONE else when (camera.trackingFailureReason) {
            TrackingFailureReason.INSUFFICIENT_LIGHT -> ArIssue.INSUFFICIENT_LIGHT
            TrackingFailureReason.EXCESSIVE_MOTION -> ArIssue.EXCESSIVE_MOTION
            TrackingFailureReason.INSUFFICIENT_FEATURES -> ArIssue.INSUFFICIENT_FEATURES
            TrackingFailureReason.CAMERA_UNAVAILABLE -> ArIssue.CAMERA_UNAVAILABLE
            TrackingFailureReason.BAD_STATE -> ArIssue.BAD_STATE
            else -> ArIssue.NONE
        }

        val estimate = frame.lightEstimate
        val lowLight = estimate.state == LightEstimate.State.VALID && estimate.pixelIntensity < LOW_LIGHT_INTENSITY

        val points = ArrayList<ArPointInfo>(placed.size)
        if (tracking) {
            camera.getProjectionMatrix(projection, 0, 0.05f, 100f)
            camera.getViewMatrix(view, 0)
            Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
        }
        for (p in placed) {
            val pose = p.anchor.pose
            world[0] = pose.tx(); world[1] = pose.ty(); world[2] = pose.tz(); world[3] = 1f
            var sx = 0f
            var sy = 0f
            var onScreen = false
            if (tracking) {
                Matrix.multiplyMV(clip, 0, viewProjection, 0, world, 0)
                if (clip[3] > 0f) {
                    val nx = clip[0] / clip[3]
                    val ny = clip[1] / clip[3]
                    sx = (nx * 0.5f + 0.5f) * viewportW
                    sy = (1f - (ny * 0.5f + 0.5f)) * viewportH
                    onScreen = abs(nx) <= 1.3f && abs(ny) <= 1.3f
                }
            }
            points.add(
                ArPointInfo(
                    Vec3(pose.tx().toDouble(), pose.ty().toDouble(), pose.tz().toDouble()),
                    p.startsNewChain, sx, sy, onScreen
                )
            )
        }

        listener?.invoke(
            ArFrameState(
                tracking = tracking,
                issue = issue,
                planeCount = planes.size,
                wallCount = wallCount,
                crosshairSurface = surface,
                lowLight = lowLight,
                points = points
            )
        )
    }

    private companion object {
        const val LOW_LIGHT_INTENSITY = 0.1f
    }
}
