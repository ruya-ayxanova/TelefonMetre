package com.smartmeter3d.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    const val TURKISH = "tr"
    const val PERSIAN = "fa"

    fun current(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) java.util.Locale.getDefault().language
        else locales[0]?.language ?: TURKISH
        return if (tag == PERSIAN) PERSIAN else TURKISH
    }

    fun set(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
