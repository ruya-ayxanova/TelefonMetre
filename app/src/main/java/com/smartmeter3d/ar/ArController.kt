package com.smartmeter3d.ar

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

/** ARCore oturumunun yaşam döngüsünü (kurulum, ayar, resume/pause, kapatma) yönetir. Depth API kullanılmaz. */
class ArController(private val appContext: Context) {
    @Volatile
    var session: Session? = null
        private set
    private var installRequested = false

    val renderer = ArRenderer(this)

    fun displayRotation(): Int {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        return wm.defaultDisplay.rotation
    }

    fun ensureSession(activity: Activity): ArSetupResult {
        if (session != null) return ArSetupResult.READY
        return try {
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return ArSetupResult.INSTALL_REQUESTED
                }
                ArCoreApk.InstallStatus.INSTALLED -> Unit
            }
            val created = Session(activity)
            val config = Config(created)
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            config.focusMode = Config.FocusMode.AUTO
            created.configure(config)
            session = created
            ArSetupResult.READY
        } catch (e: UnavailableDeviceNotCompatibleException) {
            ArSetupResult.NOT_SUPPORTED
        } catch (e: UnavailableUserDeclinedInstallationException) {
            ArSetupResult.NOT_INSTALLED
        } catch (e: UnavailableArcoreNotInstalledException) {
            ArSetupResult.NOT_INSTALLED
        } catch (e: UnavailableApkTooOldException) {
            ArSetupResult.APK_TOO_OLD
        } catch (e: UnavailableSdkTooOldException) {
            ArSetupResult.SDK_TOO_OLD
        } catch (e: Exception) {
            ArSetupResult.ERROR
        }
    }

    /** Kamerayı başlatır. Kamera açılamazsa oturum kapatılır ve false döner. */
    fun resume(): Boolean {
        val s = session ?: return false
        return try {
            s.resume()
            true
        } catch (e: CameraNotAvailableException) {
            closeQuietly(s)
            session = null
            false
        } catch (e: Exception) {
            false
        }
    }

    fun pause() {
        try {
            session?.pause()
        } catch (e: Exception) {
            // zaten duraklatılmış olabilir
        }
    }

    /** Ekran kapanırken: oturumu serbest bırakır. GL görünümü önceden duraklatılmış olmalı. */
    fun release() {
        val s = session
        session = null
        if (s != null) closeQuietly(s)
    }

    /** Yeniden kalibrasyon: oturumu kapatıp yenisini başlatır (takip ve yüzeyler sıfırlanır). GL görünümü duraklatılmış olmalı. */
    fun restart(activity: Activity): ArSetupResult {
        renderer.enqueue(ArCommand.Clear)
        val old = session
        session = null
        if (old != null) closeQuietly(old)
        val result = ensureSession(activity)
        return if (result == ArSetupResult.READY && !resume()) ArSetupResult.CAMERA_ERROR else result
    }

    fun placePoint(startsNewChain: Boolean) = renderer.enqueue(ArCommand.Place(startsNewChain))
    fun undo() = renderer.enqueue(ArCommand.Undo)
    fun clearPoints() = renderer.enqueue(ArCommand.Clear)

    private fun closeQuietly(s: Session) {
        try {
            s.pause()
        } catch (e: Exception) {
        }
        try {
            s.close()
        } catch (e: Exception) {
        }
    }
}
