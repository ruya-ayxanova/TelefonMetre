package com.smartmeter3d.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.max

object ImageLoader {
    /** Dosyayı en uzun kenarı ~[maxSide] olacak şekilde küçülterek açar. Okunamazsa null. */
    fun load(path: String, maxSide: Int = 1024): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
