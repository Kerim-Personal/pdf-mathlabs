package com.example.pdf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun applyGlobalThemeAndColor() {
        val theme = SharedPreferencesManager.getTheme(this)
        AppCompatDelegate.setDefaultNightMode(theme)
        setTheme(ThemeManager.getThemeResId(this))
        Log.d("ThemeDebug", "SettingsActivity - Tema uygulandı. Gece Modu: $theme")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ThemeDebug", "SettingsActivity - onCreate çağrıldı.")
        applyGlobalThemeAndColor()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // *** BİLDİRİM ÇUBUĞUNU GİZLEMEK İÇİN EKLENEN KOD BAŞLANGICI ***
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
        // *** BİLDİRİM ÇUBUĞUNU GİZLEMEK İÇİN EKLENEN KOD BİTİŞİ ***

        // Layout'a eklenen Toolbar'ı bul ve Action Bar olarak ayarla
        val toolbar: MaterialToolbar = findViewById(R.id.settingsToolbar)
        setSupportActionBar(toolbar)

        // Bu kod Toolbar üzerinde çalışacak ve geri tuşunu gösterecektir.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        val layoutLanguageSettings: LinearLayout = findViewById(R.id.layoutLanguageSettings)
        layoutLanguageSettings.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
        }

        val layoutThemeSettings: LinearLayout = findViewById(R.id.layoutThemeSettings)
        layoutThemeSettings.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            showThemeDialog()
        }

        val layoutAppColorSettings: LinearLayout = findViewById(R.id.layoutAppColorSettings)
        layoutAppColorSettings.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            showAppColorDialog()
        }

        setupSwitches()
    }

    private fun setupSwitches() {
        val switchTouchSound: SwitchMaterial = findViewById(R.id.switchTouchSound)
        // switchHapticFeedback ile ilgili kodlar kaldırıldı.

        switchTouchSound.isChecked = SharedPreferencesManager.isTouchSoundEnabled(this)

        switchTouchSound.setOnCheckedChangeListener { buttonView, isChecked ->
            UIFeedbackHelper.provideFeedback(buttonView)
            SharedPreferencesManager.setTouchSoundEnabled(this, isChecked)
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system_default)
        )

        val currentTheme = SharedPreferencesManager.getTheme(this)
        val checkedItem = when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.theme_title))
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val selectedTheme = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_NO
                    1 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                // Sadece mevcut temadan farklı bir seçim yapıldıysa işlem yap
                if (selectedTheme != currentTheme) {
                    SharedPreferencesManager.saveTheme(this, selectedTheme)
                    AppCompatDelegate.setDefaultNightMode(selectedTheme)
                    setResult(Activity.RESULT_OK) // Değişikliği MainActivity'ye bildir
                    recreate() // Değişikliğin hemen yansıması için Activity'yi yeniden oluştur
                }
                dialog.dismiss() // Seçim yapıldıktan sonra dialog'u kapat
            }
            .create()
            .show()
    }

    private fun showAppColorDialog() {
        val themeColorNames = Array(ThemeManager.getAppColorThemeCount()) { i ->
            ThemeManager.getAppColorThemeName(this, i)
        }

        val currentAppColorThemeIndex = SharedPreferencesManager.getAppColorTheme(this)
        var checkedItem = currentAppColorThemeIndex

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_color_title))
            .setSingleChoiceItems(themeColorNames, checkedItem) { dialog, which ->
                // Sadece mevcut renk temasından farklı bir seçim yapıldıysa işlem yap
                if (which != currentAppColorThemeIndex) {
                    ThemeManager.applyAppColorTheme(this, which) // Bu metod zaten recreate() çağırıyor
                    setResult(Activity.RESULT_OK)
                }
                dialog.dismiss() // Seçim yapıldıktan sonra dialog'u kapat
            }
            .create()
            .show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_OK)
            Log.d("ThemeDebug", "SettingsActivity - Toolbar geri tuşu: RESULT_OK ayarlandı.")
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        Log.d("ThemeDebug", "SettingsActivity - Fiziksel geri tuşu: RESULT_OK ayarlandı.")
        super.onBackPressed()
    }
}