plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)

    // Hilt
    alias(libs.plugins.dagger.hilt.android)

    // Para las anotaciones KSP (Kotlin Symbol Processors)
    alias(libs.plugins.ksp)

    // Para las anotaciones kotlin-kapt (Kotlin Annotation Plugin Tool)
    alias(libs.plugins.kotlin.kapt)

    //Navegacion segura
    alias(libs.plugins.navigation.safeargs)
}


android {
    namespace = "com.agrojurado.sfmappv2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agrojurado.sfmappv2"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    //Room
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.benchmark.common)
    implementation(libs.androidx.gridlayout)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.room.compiler)

    // Dagger - Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Codigo de barras y QR
    implementation(libs.zxing.android.embedded)
    implementation(libs.core)

    implementation(libs.play.services.location)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // okhttp3
    implementation(libs.okhttp)

    implementation(libs.androidx.work.runtime.ktx)

    implementation (libs.poi)
    implementation (libs.apache.poi.ooxml)
    implementation (libs.aalto.xml)
    implementation (libs.gcacace.signature.pad)

    // CameraX core
    implementation (libs.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)
    // Opcional: para vista previa en pantalla
    implementation (libs.androidx.camera.extensions)

    implementation (libs.coil.kt.coil)
    implementation(libs.coil.svg)

    implementation (libs.material.v190)

    // Grafico para reporte
    //implementation(libs.mpandroidchart)

    // AdMob
    //implementation(libs.play.services.ads)
    //implementation(libs.firebase.analytics)

    // Libreria
    implementation(libs.libreria.pcs)

    implementation(libs.com.itextpdf.itextg)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}