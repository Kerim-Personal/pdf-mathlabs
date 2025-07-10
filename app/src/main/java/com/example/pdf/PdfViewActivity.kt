package com.example.pdf

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch // catch operatörünü import et
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.res.ColorStateList
import androidx.core.graphics.toColorInt
import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.appbar.MaterialToolbar
import java.io.IOException
import java.io.FileNotFoundException
import androidx.appcompat.app.AppCompatDelegate
import android.util.TypedValue
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.os.Build
import com.google.android.material.card.MaterialCardView
import java.util.concurrent.TimeUnit

class PdfViewActivity : AppCompatActivity(), OnLoadCompleteListener, OnErrorListener, OnPageErrorListener {

    private lateinit var pdfView: PDFView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAiChat: FloatingActionButton
    private lateinit var fabReadingMode: FloatingActionButton
    private lateinit var eyeComfortOverlay: View
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var pdfToolbar: MaterialToolbar

    // Drawing related views and state
    private lateinit var drawingView: DrawingView
    private lateinit var fabToggleDrawing: FloatingActionButton
    private lateinit var fabEraser: FloatingActionButton

    // --- DÜZELTME BAŞLANGICI ---
    // Değişkenler doğru layout elemanlarına işaret edecek şekilde güncellendi.
    private lateinit var drawingOptionsPanel: LinearLayout // Eskiden MaterialCardView idi.
    // --- DÜZELTME BİTİŞİ ---

    private lateinit var colorOptions: LinearLayout
    private lateinit var sizeOptions: LinearLayout
    private lateinit var clearAllButtonContainer: LinearLayout

    private lateinit var btnColorRed: ImageButton
    private lateinit var btnColorBlue: ImageButton
    private lateinit var btnColorBlack: ImageButton

    private lateinit var btnSizeSmall: ImageButton
    private lateinit var btnSizeMedium: ImageButton
    private lateinit var btnSizeLarge: ImageButton

    private lateinit var fabClearAll: FloatingActionButton

    private var isDrawingActive: Boolean = false
    private var currentPenColor: Int = Color.RED
    private var currentPenSize: Float = 10f
    private var currentEraserSize: Float = 50f

    private var pdfAssetName: String? = null
    private var fullPdfText: String? = null

    private var currentReadingModeLevel: Int = 0

    private val generativeModel by lazy {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            Log.e("GeminiAI", "API Anahtarı BuildConfig içerisinde bulunamadı veya geçersiz. Lütfen local.properties dosyasını ve build.gradle.kts yapılandırmasını kontrol edin.")
            showSnackbar(getString(R.string.ai_assistant_api_key_not_configured))
        }
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    companion object {
        const val EXTRA_PDF_ASSET_NAME = "pdf_asset_name"
        const val EXTRA_PDF_TITLE = "pdf_title"
        private const val GEMINI_API_CALL_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun applyThemeAndColor() {
        val selectedColorThemeIndex = SharedPreferencesManager.getAppColorTheme(this)
        val currentNightMode = SharedPreferencesManager.getTheme(this)

        val themeResId = when (selectedColorThemeIndex) {
            0 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_SereneBlue_Dark else R.style.Theme_Pdf_SereneBlue_Light
            1 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_Red_Dark else R.style.Theme_Pdf_Red_Light
            2 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_Green_Dark else R.style.Theme_Pdf_Green_Light
            3 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_Purple_Dark else R.style.Theme_Pdf_Purple_Light
            4 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_Orange_Dark else R.style.Theme_Pdf_Orange_Light
            5 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_DeepPurple_Dark else R.style.Theme_Pdf_DeepPurple_Light
            6 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_Indigo_Dark else R.style.Theme_Pdf_Indigo_Light
            7 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_Cyan_Dark else R.style.Theme_Pdf_Cyan_Light
            8 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_Pink_Dark else R.style.Theme_Pdf_Pink_Light
            9 -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_Brown_Dark else R.style.Theme_Pdf_Brown_Light
            else -> if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) R.style.Theme_Pdf_SereneBlue_Dark else R.style.Theme_Pdf_SereneBlue_Light
        }
        setTheme(themeResId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeAndColor()
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_pdf_view)

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

        rootLayout = findViewById(R.id.root_layout_pdf_view)
        pdfToolbar = findViewById(R.id.pdfToolbar)

        setSupportActionBar(pdfToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        pdfAssetName = intent.getStringExtra(EXTRA_PDF_ASSET_NAME)
        val pdfTitle = intent.getStringExtra(EXTRA_PDF_TITLE) ?: getString(R.string.app_name)

        supportActionBar?.title = pdfTitle

        pdfView = findViewById(R.id.pdfView)
        progressBar = findViewById(R.id.progressBarPdf)
        fabAiChat = findViewById(R.id.fab_ai_chat)
        fabReadingMode = findViewById(R.id.fab_reading_mode)
        eyeComfortOverlay = findViewById(R.id.eyeComfortOverlay)

        drawingView = findViewById(R.id.drawingView)
        fabToggleDrawing = findViewById(R.id.fab_toggle_drawing)
        fabEraser = findViewById(R.id.fab_eraser)

        // --- DÜZELTME BAŞLANGICI ---
        // Değişkenler doğru layout ID'lerine atandı.
        drawingOptionsPanel = findViewById(R.id.drawingOptionsPanel)
        // --- DÜZELTME BİTİŞİ ---

        colorOptions = findViewById(R.id.colorOptions)
        sizeOptions = findViewById(R.id.sizeOptions)
        clearAllButtonContainer = findViewById(R.id.clearAllButtonContainer)

        btnColorRed = findViewById(R.id.btn_color_red)
        btnColorBlue = findViewById(R.id.btn_color_blue)
        btnColorBlack = findViewById(R.id.btn_color_black)

        btnSizeSmall = findViewById(R.id.btn_size_small)
        btnSizeMedium = findViewById(R.id.btn_size_medium)
        btnSizeLarge = findViewById(R.id.btn_size_large)

        fabClearAll = findViewById(R.id.fab_clear_all)

        if (pdfAssetName != null) {
            displayPdfFromAssets(pdfAssetName!!)
        } else {
            showSnackbar(getString(R.string.pdf_not_found))
            finish()
        }

        fabAiChat.setOnClickListener {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty()) {
                showSnackbar(getString(R.string.ai_assistant_api_key_not_configured))
                return@setOnClickListener
            }

            val isFirstCall = SharedPreferencesManager.getIsFirstGeminiApiCall(this)
            val currentTime = System.currentTimeMillis()

            showAiChatDialog()
            if (isFirstCall) {
                SharedPreferencesManager.setIsFirstGeminiApiCall(this, false)
            }
            SharedPreferencesManager.saveLastGeminiApiCallTimestamp(this, currentTime)
        }

        fabReadingMode.setOnClickListener {
            toggleReadingMode()
            UIFeedbackHelper.provideFeedback(it)
        }

        drawingView.drawingMode = DrawingView.DrawingMode.NONE
        setDrawingButtonState(false)

        fabToggleDrawing.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            toggleDrawingMode()
        }

        fabEraser.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            activateEraserMode()
        }

        fabClearAll.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            drawingView.clearDrawing()
            showSnackbar(getString(R.string.all_drawings_cleared_toast))
        }

        setupDrawingOptions()
    }

    // ONDESTROY METODU GÜNCELLENDİ
    override fun onDestroy() {
        super.onDestroy()
        // UIFeedbackHelper.release() satırı buradan kaldırıldı.
    }

    private fun setupDrawingOptions() {
        currentPenColor = SharedPreferencesManager.getPenColor(this)
        currentPenSize = when (SharedPreferencesManager.getPenSizeType(this)) {
            DrawingModeType.SMALL.ordinal -> 5f
            DrawingModeType.MEDIUM.ordinal -> 10f
            DrawingModeType.LARGE.ordinal -> 20f
            else -> 10f
        }
        currentEraserSize = when (SharedPreferencesManager.getEraserSizeType(this)) {
            DrawingModeType.SMALL.ordinal -> 25f
            DrawingModeType.MEDIUM.ordinal -> 50f
            DrawingModeType.LARGE.ordinal -> 75f
            else -> 50f
        }

        drawingView.setBrushColor(currentPenColor)

        when (currentPenColor) {
            ContextCompat.getColor(this, R.color.red) -> updateColorSelection(btnColorRed)
            ContextCompat.getColor(this, R.color.blue) -> updateColorSelection(btnColorBlue)
            ContextCompat.getColor(this, R.color.black) -> updateColorSelection(btnColorBlack)
            else -> updateColorSelection(btnColorRed)
        }

        when (SharedPreferencesManager.getPenSizeType(this)) {
            DrawingModeType.SMALL.ordinal -> updateSizeSelection(btnSizeSmall)
            DrawingModeType.MEDIUM.ordinal -> updateSizeSelection(btnSizeMedium)
            DrawingModeType.LARGE.ordinal -> updateSizeSelection(btnSizeLarge)
            else -> updateSizeSelection(btnSizeMedium)
        }

        btnColorRed.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            currentPenColor = ContextCompat.getColor(this, R.color.red)
            drawingView.setBrushColor(currentPenColor)
            SharedPreferencesManager.savePenColor(this, currentPenColor)
            updateColorSelection(it)
        }
        btnColorBlue.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            currentPenColor = ContextCompat.getColor(this, R.color.blue)
            drawingView.setBrushColor(currentPenColor)
            SharedPreferencesManager.savePenColor(this, currentPenColor)
            updateColorSelection(it)
        }
        btnColorBlack.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            currentPenColor = ContextCompat.getColor(this, R.color.black)
            drawingView.setBrushColor(currentPenColor)
            SharedPreferencesManager.savePenColor(this, currentPenColor)
            updateColorSelection(it)
        }

        btnSizeSmall.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            setBrushOrEraserSize(5f, DrawingModeType.SMALL)
            updateSizeSelection(it)
        }
        btnSizeMedium.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            setBrushOrEraserSize(10f, DrawingModeType.MEDIUM)
            updateSizeSelection(it)
        }
        btnSizeLarge.setOnClickListener {
            UIFeedbackHelper.provideFeedback(it)
            setBrushOrEraserSize(20f, DrawingModeType.LARGE)
            updateSizeSelection(it)
        }
    }

    private fun setBrushOrEraserSize(size: Float, type: DrawingModeType) {
        if (drawingView.drawingMode == DrawingView.DrawingMode.PEN) {
            currentPenSize = size
            drawingView.setBrushSize(currentPenSize)
            SharedPreferencesManager.savePenSizeType(this, type.ordinal)
        } else if (drawingView.drawingMode == DrawingView.DrawingMode.ERASER) {
            currentEraserSize = size
            drawingView.setBrushSize(currentEraserSize)
            SharedPreferencesManager.saveEraserSizeType(this, type.ordinal)
        }
    }

    private fun updateColorSelection(selectedView: View) {
        btnColorRed.isSelected = false
        btnColorBlue.isSelected = false
        btnColorBlack.isSelected = false
        selectedView.isSelected = true
    }

    private fun updateSizeSelection(selectedView: View) {
        btnSizeSmall.isSelected = false
        btnSizeMedium.isSelected = false
        btnSizeLarge.isSelected = false
        selectedView.isSelected = true
    }

    private fun toggleReadingMode() {
        currentReadingModeLevel = (currentReadingModeLevel + 1) % 4
        applyReadingModeFilter(currentReadingModeLevel)
    }

    private fun applyReadingModeFilter(level: Int) {
        when (level) {
            0 -> {
                eyeComfortOverlay.visibility = View.GONE
                eyeComfortOverlay.setBackgroundColor(Color.TRANSPARENT)
                showSnackbar(getString(R.string.reading_mode_off_toast))
            }
            1 -> {
                eyeComfortOverlay.visibility = View.VISIBLE
                eyeComfortOverlay.setBackgroundColor("#33FDF6E3".toColorInt())
                showSnackbar(getString(R.string.reading_mode_low_toast))
            }
            2 -> {
                eyeComfortOverlay.visibility = View.VISIBLE
                eyeComfortOverlay.setBackgroundColor("#66FDF6E3".toColorInt())
                showSnackbar(getString(R.string.reading_mode_medium_toast))
            }
            3 -> {
                eyeComfortOverlay.visibility = View.VISIBLE
                eyeComfortOverlay.setBackgroundColor("#99FDF6E3".toColorInt())
                showSnackbar(getString(R.string.reading_mode_high_toast))
            }
        }
    }

    private fun toggleDrawingMode() {
        if (isDrawingActive && drawingView.drawingMode == DrawingView.DrawingMode.PEN) {
            drawingView.drawingMode = DrawingView.DrawingMode.NONE
            isDrawingActive = false
            drawingOptionsPanel.visibility = View.GONE
            clearAllButtonContainer.visibility = View.GONE
            showSnackbar(getString(R.string.drawing_mode_off_toast))
        } else {
            drawingView.drawingMode = DrawingView.DrawingMode.PEN
            isDrawingActive = true
            drawingView.setBrushColor(currentPenColor)
            drawingView.setBrushSize(currentPenSize)
            drawingOptionsPanel.visibility = View.VISIBLE
            colorOptions.visibility = View.VISIBLE
            sizeOptions.visibility = View.VISIBLE
            clearAllButtonContainer.visibility = View.VISIBLE

            when (SharedPreferencesManager.getPenSizeType(this)) {
                DrawingModeType.SMALL.ordinal -> updateSizeSelection(btnSizeSmall)
                DrawingModeType.MEDIUM.ordinal -> updateSizeSelection(btnSizeMedium)
                DrawingModeType.LARGE.ordinal -> updateSizeSelection(btnSizeLarge)
                else -> updateSizeSelection(btnSizeMedium)
            }

            val savedColor = SharedPreferencesManager.getPenColor(this)
            when (savedColor) {
                ContextCompat.getColor(this, R.color.red) -> updateColorSelection(btnColorRed)
                ContextCompat.getColor(this, R.color.blue) -> updateColorSelection(btnColorBlue)
                ContextCompat.getColor(this, R.color.black) -> updateColorSelection(btnColorBlack)
                else -> updateColorSelection(btnColorRed)
            }

            showSnackbar(getString(R.string.drawing_mode_pencil_toast))
        }
        setDrawingButtonState(isDrawingActive)
    }

    private fun activateEraserMode() {
        if (isDrawingActive && drawingView.drawingMode == DrawingView.DrawingMode.ERASER) {
            drawingView.drawingMode = DrawingView.DrawingMode.NONE
            isDrawingActive = false
            drawingOptionsPanel.visibility = View.GONE
            clearAllButtonContainer.visibility = View.GONE
            showSnackbar(getString(R.string.drawing_mode_off_toast))
        } else {
            drawingView.drawingMode = DrawingView.DrawingMode.ERASER
            isDrawingActive = true
            drawingView.setBrushSize(currentEraserSize)
            drawingOptionsPanel.visibility = View.VISIBLE
            colorOptions.visibility = View.GONE
            sizeOptions.visibility = View.VISIBLE
            clearAllButtonContainer.visibility = View.VISIBLE

            when (SharedPreferencesManager.getEraserSizeType(this)) {
                DrawingModeType.SMALL.ordinal -> updateSizeSelection(btnSizeSmall)
                DrawingModeType.MEDIUM.ordinal -> updateSizeSelection(btnSizeMedium)
                DrawingModeType.LARGE.ordinal -> updateSizeSelection(btnSizeLarge)
                else -> updateSizeSelection(btnSizeMedium)
            }
            showSnackbar(getString(R.string.drawing_mode_eraser_toast))
        }
        setDrawingButtonState(isDrawingActive)
    }

    private fun setDrawingButtonState(active: Boolean) {
        // Renkleri doğrudan tanımlıyoruz: Aktif için Siyah, Pasif için Beyaz.
        val activeColor = ColorStateList.valueOf(Color.BLACK)
        val inactiveColor = ColorStateList.valueOf(Color.WHITE)

        if (active) {
            if (drawingView.drawingMode == DrawingView.DrawingMode.PEN) {
                // Kalem modu aktif: Kalem ikonu siyah, silgi ikonu beyaz.
                fabToggleDrawing.imageTintList = activeColor
                fabEraser.imageTintList = inactiveColor
            } else if (drawingView.drawingMode == DrawingView.DrawingMode.ERASER) {
                // Silgi modu aktif: Kalem ikonu beyaz, silgi ikonu siyah.
                fabToggleDrawing.imageTintList = inactiveColor
                fabEraser.imageTintList = activeColor
            }
        } else {
            // Hiçbir mod aktif değil: Her iki ikon da beyaz.
            fabToggleDrawing.imageTintList = inactiveColor
            fabEraser.imageTintList = inactiveColor
        }
    }

    private fun displayPdfFromAssets(assetName: String) {
        progressBar.visibility = View.VISIBLE
        try {
            pdfView.fromAsset(assetName)
                .defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .onLoad(this)
                .onError(this)
                .onPageError(this)
                .load()
        } catch (e: FileNotFoundException) {
            progressBar.visibility = View.GONE
            Log.e("PdfViewError", "PDF dosyası bulunamadı: $assetName - ${e.localizedMessage}")
            showSnackbar(getString(R.string.pdf_not_found))
            finish()
        } catch (e: IOException) {
            progressBar.visibility = View.GONE
            Log.e("PdfViewError", "PDF okuma/yükleme hatası: $assetName - ${e.localizedMessage}")
            showSnackbar(getString(R.string.pdf_load_failed_with_error, e.localizedMessage ?: "Dosya okuma hatası"))
            finish()
        }
        catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e("PdfViewError", "PDF yüklenirken genel hata: ${e.localizedMessage}", e)
            showSnackbar(getString(R.string.pdf_load_failed_with_error, e.localizedMessage ?: "Bilinmeyen hata"))
            finish()
        }
    }

    private fun showAiChatDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_ai_chat, null)
        builder.setView(dialogView)

        val editTextQuestion = dialogView.findViewById<EditText>(R.id.editTextQuestion)
        val buttonSend = dialogView.findViewById<Button>(R.id.buttonSend)
        val textViewAnswer = dialogView.findViewById<TextView>(R.id.textViewAnswer)
        val progressChat = dialogView.findViewById<ProgressBar>(R.id.progressChat)

        val dialog = builder.create()

        buttonSend.setOnClickListener {
            val question = editTextQuestion.text.toString().trim()
            if (question.isNotEmpty()) {
                textViewAnswer.text = ""
                textViewAnswer.visibility = View.GONE
                progressChat.visibility = View.VISIBLE
                buttonSend.isEnabled = false
                editTextQuestion.isEnabled = false

                lifecycleScope.launch {
                    try {
                        val prompt = """
                        Kullanıcının sorusunu genel bilginize dayanarak en fazla 100 karakter uzunluğunda yanıtlayın.
                        Cevabınızı Markdown formatında (örneğin, başlıklar için #, alt başlıklar için ##, listeler için * veya - kullanarak) ve Türkçe olarak verin.
                        Sadece net ve öz cevaplar verin, gereksiz detaylardan kaçının. Cevabınız 100 karakteri kesinlikle aşmasın.

                        Kullanıcının Sorusu: "$question"
                        """.trimIndent()

                        val responseFlow = generativeModel.generateContentStream(prompt)
                            .catch { e ->
                                Log.e("GeminiError", "API çağrısı hatası: ${e.localizedMessage}", e)
                                withContext(Dispatchers.Main) {
                                    textViewAnswer.text = getString(R.string.ai_chat_error_with_details, e.localizedMessage ?: "Unknown error")
                                    textViewAnswer.visibility = View.VISIBLE
                                    progressChat.visibility = View.GONE
                                    buttonSend.isEnabled = true
                                    editTextQuestion.isEnabled = true
                                }
                            }


                        val stringBuilder = StringBuilder()
                        responseFlow.collect { chunk ->
                            Log.d("GeminiResponse", "Received chunk: ${chunk.text}")
                            if (stringBuilder.length < 100) {
                                stringBuilder.append(chunk.text)
                                if (stringBuilder.length > 100) {
                                    stringBuilder.setLength(100)
                                }
                            }
                        }
                        textViewAnswer.text = stringBuilder.toString()
                        textViewAnswer.visibility = View.VISIBLE

                    } catch (e: Exception) {
                        textViewAnswer.text = getString(R.string.ai_chat_error_with_details, e.localizedMessage ?: "Unknown error")
                        Log.e("GeminiError", "AI Hatası (genel yakalama): ", e)
                        textViewAnswer.visibility = View.VISIBLE
                    } finally {
                        progressChat.visibility = View.GONE
                        buttonSend.isEnabled = true
                        editTextQuestion.isEnabled = true
                    }
                }
            } else {
                showSnackbar(getString(R.string.please_enter_a_question))
            }
        }
        dialog.show()
    }

    @Suppress("unused")
    private fun extractTextFromPdf(assetName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                PDFBoxResourceLoader.init(applicationContext)

                assets.open(assetName).use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        val stripper = PDFTextStripper()
                        val text = stripper.getText(document)
                        withContext(Dispatchers.Main) {
                            fullPdfText = text
                            val apiKey = BuildConfig.GEMINI_API_KEY
                            if (apiKey.isNotEmpty()) {
                                val fadeInAnimation = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in)
                                fabAiChat.startAnimation(fadeInAnimation)
                                fabAiChat.visibility = View.VISIBLE
                            }
                            Log.d("PdfTextExtraction", "Metin başarıyla çıkarıldı. Karakter sayısı: ${text.length}")
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                withContext(Dispatchers.Main) {
                    showSnackbar(getString(R.string.pdf_not_found))
                    Log.e("PdfTextExtraction", "Metin çıkarılırken dosya bulunamadı: $assetName", e)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showSnackbar(getString(R.string.pdf_load_failed_with_error, e.localizedMessage ?: "Dosya okuma hatası"))
                    Log.e("PdfTextExtraction", "Metin çıkarılırken IO hatası: $assetName", e)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar(getString(R.string.pdf_text_extraction_failed, e.localizedMessage ?: "Bilinmeyen hata"))
                    Log.e("PdfTextExtraction", "Metin çıkarılırken genel hata: ${e.localizedMessage}", e)
                }
            }
        }
    }

    override fun loadComplete(nbPages: Int) {
        progressBar.visibility = View.GONE
        showSnackbar(getString(R.string.pdf_loaded_toast, nbPages))
        pdfAssetName?.let {
            if (fullPdfText == null) {
                extractTextFromPdf(it)
            }
        }
    }

    override fun onError(t: Throwable?) {
        progressBar.visibility = View.GONE
        showSnackbar(getString(R.string.error_toast, t?.localizedMessage ?: "Bilinmeyen PDF hatası"))
        Log.e("PdfView_onError", "PDF Yükleme Hatası", t)
        finish()
    }

    override fun onPageError(page: Int, t: Throwable?) {
        showSnackbar(getString(R.string.page_load_error_toast, page, t?.localizedMessage ?: "Bilinmeyen sayfa hatası"))
        Log.e("PdfView_onPageError", "Sayfa Yükleme Hatası: $page", t)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}

enum class DrawingModeType {
    SMALL, MEDIUM, LARGE
}