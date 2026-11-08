package com.smartmeter3d.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import kotlin.math.max

class PhotoStorage(private val context: Context) {
    private fun sourceDir(): File = File(context.cacheDir, "photos").apply { mkdirs() }
    private fun measuredDir(): File = File(context.filesDir, "measured").apply { mkdirs() }

    fun newCaptureFile(): File = File(sourceDir(), "capture_${System.currentTimeMillis()}.jpg")

    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Galeriden seçilen görüntüyü geçici dosyaya kopyalar. Başarısızsa null. */
    fun importFromUri(uri: Uri): File? {
        val target = File(sourceDir(), "import_${System.currentTimeMillis()}.jpg")
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { stream -> target.outputStream().use { out -> stream.copyTo(out) } }
            target
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Büyük fotoğrafları küçülterek (en uzun kenar ≤ [maxSide]) ve EXIF yönünü uygulayarak açar. */
    fun decode(file: File, maxSide: Int = 2048): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null

        val orientation = try {
            ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return raw
        }
        return try {
            val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            if (rotated !== raw) raw.recycle()
            rotated
        } catch (e: OutOfMemoryError) {
            raw
        }
    }

    /** Ölçüm bilgileri çizilmiş fotoğrafı kalıcı klasöre JPEG olarak kaydeder. */
    @Throws(IOException::class)
    fun saveAnnotated(bitmap: Bitmap): File {
        val file = File(measuredDir(), "measure_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) throw IOException("compress failed")
        }
        return file
    }
}
