plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firma de release opcional: solo se activa si el workflow de releases define estas
// variables (a partir de los secrets del repositorio). Sin ellas, el release se
// compila sin firmar, como hasta ahora.
val releaseKeystorePath: String? = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull

android {
    namespace = "com.julen.clases"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.julen.clases"
        minSdk = 24
        targetSdk = 37
        // El workflow de releases lo calcula desde la etiqueta (v3.1.2 -> 30102).
        versionCode = providers.gradleProperty("versionCode").orNull?.toIntOrNull() ?: 30000
        // El workflow de releases puede fijarlo desde la etiqueta (-PversionName=2.1).
        versionName = providers.gradleProperty("versionName").orNull ?: "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}