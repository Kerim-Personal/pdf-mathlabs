import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
    try {
        localProperties.load(FileInputStream(localPropertiesFile))
    } catch (e: Exception) {
        println("Warning: Could not load local.properties: ${e.message}")
    }
} else {
    println("Warning: local.properties file not found in project root. Please ensure it exists and contains your GEMINI_API_KEY.")
}

android {
    namespace = "com.example.pdf"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pdf"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API anahtarını BuildConfig'a ekleme
        val geminiApiKeyFromProperties = localProperties.getProperty("GEMINI_API_KEY")?.trim() ?: ""

        if (geminiApiKeyFromProperties.isEmpty()) {
            println("Warning: GEMINI_API_KEY is empty in local.properties. AI features may not work.")
            // Anahtar boşsa, BuildConfig'a boş bir string ata
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
        } else {
            println("Info: GEMINI_API_KEY loaded from local.properties.")
            // Anahtar doluysa, değeri doğru şekilde escape edilmiş bir string olarak ata.
            // Java'da bir string sabiti oluşturmak için değeri çift tırnak içine alıyoruz.
            // Eğer anahtarın kendisi çift tırnak veya ters eğik çizgi gibi özel karakterler içeriyorsa,
            // bu karakterlerin escape edilmesi gerekir.
            // Ancak, Google API anahtarları genellikle bu tür karakterleri içermez.
            // En yaygın senaryo için:
            buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKeyFromProperties.replace("\"", "\\\"")}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val geminiApiKeyFromProperties = localProperties.getProperty("GEMINI_API_KEY")?.trim() ?: ""
            if (geminiApiKeyFromProperties.isNotEmpty()) {
                buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKeyFromProperties.replace("\"", "\\\"")}\"")
            } else {
                buildConfigField("String", "GEMINI_API_KEY", "\"\"")
            }
        }
        debug {} // defaultConfig'ten miras alır
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.android.pdf.viewer)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity)

    implementation("com.google.ai.client.generativeai:generativeai:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
