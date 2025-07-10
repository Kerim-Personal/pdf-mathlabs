package com.example.pdf

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PdfApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Uygulama genelinde varsayılan gece modunu ayarla
        AppCompatDelegate.setDefaultNightMode(SharedPreferencesManager.getTheme(this))

        // Ses ve titreşim yardımcısını uygulama genelinde başlat
        UIFeedbackHelper.init(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let { LocaleHelper.onAttach(it) })
    }
}