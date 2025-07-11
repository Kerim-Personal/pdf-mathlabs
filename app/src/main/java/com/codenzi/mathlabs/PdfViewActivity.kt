package com.codenzi.mathlabs

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

class PdfViewActivity : AppCompatActivity(), OnLoadCompleteListener, OnErrorListener, OnPageErrorListener {

    private lateinit var pdfView: PDFView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAiChat: FloatingActionButton
    private lateinit var fabReadingMode: FloatingActionButton
    private lateinit var eyeComfortOverlay: View
    private lateinit var pdfToolbar: MaterialToolbar
    private lateinit var notificationTextView: TextView

    // DrawingManager bu sınıfın tüm çizim sorumluluğunu alacak
    private lateinit var drawingManager: DrawingManager

    private var pdfAssetName: String? = null
    private var fullPdfText: String? = null
    private var currentReadingModeLevel: Int = 0

    private val toastHandler = Handler(Looper.getMainLooper())
    private var toastRunnable: Runnable? = null


    private val generativeModel by lazy {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            Log.e("GeminiAI", "API Anahtarı BuildConfig içerisinde bulunamadı veya geçersiz.")
            showAnimatedToast(getString(R.string.ai_assistant_api_key_not_configured))
        }
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    companion object {
        const val EXTRA_PDF_ASSET_NAME = "pdf_asset_name"
        const val EXTRA_PDF_TITLE = "pdf_title"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun applyThemeAndColor() {
        setTheme(ThemeManager.getThemeResId(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeAndColor()
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_pdf_view)

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        setupToolbar()
        initializeViews()
        setupListeners()

        pdfAssetName = intent.getStringExtra(EXTRA_PDF_ASSET_NAME)
        val pdfTitle = intent.getStringExtra(EXTRA_PDF_TITLE) ?: getString(R.string.app_name)
        supportActionBar?.title = pdfTitle

        if (pdfAssetName != null) {
            displayPdfFromAssets(pdfAssetName!!)
        } else {
            showAnimatedToast(getString(R.string.pdf_not_found))
            finish()
        }
    }

    private fun setupToolbar() {
        pdfToolbar = findViewById(R.id.pdfToolbar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(pdfToolbar) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBarInsets.top
            }
            insets
        }
        setSupportActionBar(pdfToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initializeViews() {
        pdfView = findViewById(R.id.pdfView)
        progressBar = findViewById(R.id.progressBarPdf)
        fabAiChat = findViewById(R.id.fab_ai_chat)
        fabReadingMode = findViewById(R.id.fab_reading_mode)
        eyeComfortOverlay = findViewById(R.id.eyeComfortOverlay)
        notificationTextView = findViewById(R.id.notificationTextView)

        drawingManager = DrawingManager(
            context = this,
            drawingView = findViewById(R.id.drawingView),
            fabToggleDrawing = findViewById(R.id.fab_toggle_drawing),
            fabEraser = findViewById(R.id.fab_eraser),
            fabClearAll = findViewById(R.id.fab_clear_all),
            drawingOptionsPanel = findViewById(R.id.drawingOptionsPanel),
            colorOptions = findViewById(R.id.colorOptions),
            sizeOptions = findViewById(R.id.sizeOptions),
            btnColorRed = findViewById(R.id.btn_color_red),
            btnColorBlue = findViewById(R.id.btn_color_blue),
            btnColorBlack = findViewById(R.id.btn_color_black),
            btnSizeSmall = findViewById(R.id.btn_size_small),
            btnSizeMedium = findViewById(R.id.btn_size_medium),
            btnSizeLarge = findViewById(R.id.btn_size_large),
            showSnackbar = { message -> showAnimatedToast(message) }
        )
    }

    private fun setupListeners() {
        fabAiChat.setOnClickListener {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty()) {
                showAnimatedToast(getString(R.string.ai_assistant_api_key_not_configured))
                return@setOnClickListener
            }
            showAiChatDialog()
        }

        fabReadingMode.setOnClickListener {
            toggleReadingMode()
            UIFeedbackHelper.provideFeedback(it)
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
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            val errorMessage = when (e) {
                is FileNotFoundException -> getString(R.string.pdf_not_found)
                is IOException -> getString(R.string.pdf_load_failed_with_error, e.localizedMessage)
                else -> getString(R.string.pdf_load_failed_with_error, e.localizedMessage ?: "Bilinmeyen hata")
            }
            Log.e("PdfViewError", "PDF yüklenirken hata: $assetName", e)
            showAnimatedToast(errorMessage)
            finish()
        }
    }

    private fun toggleReadingMode() {
        currentReadingModeLevel = (currentReadingModeLevel + 1) % 4
        applyReadingModeFilter(currentReadingModeLevel)
    }

    private fun applyReadingModeFilter(level: Int) {
        when (level) {
            0 -> {
                eyeComfortOverlay.visibility = View.GONE
                showAnimatedToast(getString(R.string.reading_mode_off_toast))
            }
            1 -> {
                eyeComfortOverlay.visibility = View.VISIBLE
                eyeComfortOverlay.setBackgroundColor("#33FDF6E3".toColorInt())
                showAnimatedToast(getString(R.string.reading_mode_low_toast))
            }
            2 -> {
                eyeComfortOverlay.visibility = View.VISIBLE
                eyeComfortOverlay.setBackgroundColor("#66FDF6E3".toColorInt())
                showAnimatedToast(getString(R.string.reading_mode_medium_toast))
            }
            3 -> {
                eyeComfortOverlay.visibility = View.VISIBLE
                eyeComfortOverlay.setBackgroundColor("#99FDF6E3".toColorInt())
                showAnimatedToast(getString(R.string.reading_mode_high_toast))
            }
        }
    }

    private fun showAiChatDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_chat, null)
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
                        Cevabınızı Markdown formatında ve Türkçe olarak verin.
                        Sadece net ve öz cevaplar verin.
                        Kullanıcının Sorusu: "$question"
                        """.trimIndent()

                        val responseFlow = generativeModel.generateContentStream(prompt)
                            .catch { e ->
                                withContext(Dispatchers.Main) {
                                    textViewAnswer.text = getString(R.string.ai_chat_error_with_details, e.localizedMessage ?: "Unknown error")
                                    textViewAnswer.visibility = View.VISIBLE
                                }
                            }

                        val stringBuilder = StringBuilder()
                        responseFlow.collect { chunk ->
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
                        Log.e("GeminiError", "AI Hatası: ", e)
                        textViewAnswer.visibility = View.VISIBLE
                    } finally {
                        progressChat.visibility = View.GONE
                        buttonSend.isEnabled = true
                        editTextQuestion.isEnabled = true
                    }
                }
            } else {
                showAnimatedToast(getString(R.string.please_enter_a_question))
            }
        }
        dialog.show()
    }

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
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = when (e) {
                        is FileNotFoundException -> getString(R.string.pdf_not_found)
                        else -> getString(R.string.pdf_text_extraction_failed, e.localizedMessage)
                    }
                    Log.e("PdfTextExtraction", "Metin çıkarılırken hata: $assetName", e)
                    showAnimatedToast(errorMessage)
                }
            }
        }
    }

    override fun loadComplete(nbPages: Int) {
        progressBar.visibility = View.GONE
        showAnimatedToast(getString(R.string.pdf_loaded_toast, nbPages))
        pdfAssetName?.let {
            if (fullPdfText == null) {
                extractTextFromPdf(it)
            }
        }
    }

    override fun onError(t: Throwable?) {
        progressBar.visibility = View.GONE
        showAnimatedToast(getString(R.string.error_toast, t?.localizedMessage ?: "Bilinmeyen PDF hatası"))
        Log.e("PdfView_onError", "PDF Yükleme Hatası", t)
        finish()
    }

    override fun onPageError(page: Int, t: Throwable?) {
        showAnimatedToast(getString(R.string.page_load_error_toast, page, t?.localizedMessage ?: "Bilinmeyen sayfa hatası"))
        Log.e("PdfView_onPageError", "Sayfa Yükleme Hatası: $page", t)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun showAnimatedToast(message: String) {
        // Önceki zamanlayıcıyı iptal et
        toastRunnable?.let { toastHandler.removeCallbacks(it) }

        notificationTextView.text = message

        // Fade in animasyonu
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        notificationTextView.startAnimation(fadeIn)
        notificationTextView.visibility = View.VISIBLE

        // Fade out için zamanlayıcı
        toastRunnable = Runnable {
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    notificationTextView.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            notificationTextView.startAnimation(fadeOut)
        }

        toastHandler.postDelayed(toastRunnable!!, 2000) // 2 saniye sonra fade out
    }
}

enum class DrawingModeType {
    SMALL, MEDIUM, LARGE
}