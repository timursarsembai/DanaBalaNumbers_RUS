plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.timursarsembayev.danabalanumbers"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.timursarsembayev.danabalanumbers"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Используем реальный App ID для всех сборок
        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-8956179513137325~7603355552"
    }

    buildTypes {
        debug {
            // Реальный App ID также в debug
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-8956179513137325~7603355552"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Реальный App ID для релиза
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-8956179513137325~7603355552"
        }
    }
    compileOptions {
        // Updated to Java 17 for Kotlin 2.1.x & modern AGP requirements
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Fragment
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Coroutines (updated for Kotlin 2.1.x compatibility)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // UI Components
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Google Play Billing (обновлено до 8.0.0 — дальнейшая адаптация к API выполнена)
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    // Google Mobile Ads SDK (AdMob)
    // Используем стабильную версию 23.6.0 для совместимости
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}