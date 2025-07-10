package com.example.pdf

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class LanguageSelectionActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun applyThemeAndColor() {
        // Merkezi tema yöneticisinden doğru temayı al ve uygula
        setTheme(ThemeManager.getThemeResId(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeAndColor() // Bu çağrı super.onCreate'den önce olmalı
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)

        // Durum çubuğunu gizleme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        val layoutTurkish: LinearLayout = findViewById(R.id.layoutTurkish)
        val layoutEnglish: LinearLayout = findViewById(R.id.layoutEnglish)

        layoutTurkish.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            setLanguageAndProceed("tr")
        }

        layoutEnglish.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            setLanguageAndProceed("en")
        }
    }

    private fun setLanguageAndProceed(languageCode: String) {
        LocaleHelper.persist(this, languageCode)

        val intent: Intent
        if (SharedPreferencesManager.getUserName(this) == null) {
            intent = Intent(this, NameEntryActivity::class.java)
        } else {
            intent = Intent(this, MainActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}