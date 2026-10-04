import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Порядковый номер сборки. Передаётся из CI: -PbuildNumber=<номер запуска>.
// Локально (без параметра) используется 1.
val buildNumber: Int = (providers.gradleProperty("buildNumber").orNull ?: "1").toInt()

// Свойства подписи. Ключ хранится в репозитории (release.keystore),
// чтобы каждая CI-сборка подписывалась одним и тем же постоянным ключом.
val keystoreProperties = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) {
        keystoreFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.techpulse.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.techpulse.app"
        minSdk = 26
        targetSdk = 34

        // Порядковый номер сборки попадает и в versionCode, и в versionName.
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"

        buildConfigField("int", "BUILD_NUMBER", buildNumber.toString())
        // Репозиторий для проверки обновлений (GitHub Releases)
        buildConfigField("String", "GITHUB_REPO", "\"zigorminsk-debug/NEWS\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storeType = keystoreProperties.getProperty("storeType", "pkcs12")
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Если ключ недоступен — подписываем debug-ключом, чтобы сборка не падала.
            signingConfig = if (keystoreProperties.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Chrome Custom Tabs — открытие источника новости прямо в приложении
    implementation(libs.androidx.browser)

    // Загрузка изображений из RSS
    implementation(libs.coil.compose)

    // Извлечение текста статей для встроенного ридера
    implementation(libs.jsoup)

    // HTTP-клиент для загрузки RSS-лент
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
