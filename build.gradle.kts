// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    // Hilt
    alias(libs.plugins.dagger.hilt.android) apply false

    // Para las anotaciones KSP (Kotlin Symbol Processors)
    alias(libs.plugins.ksp) apply false

    // Para las anotaciones kotlin-kapt (Kotlin Annotation Plugin Tool)
    alias(libs.plugins.kotlin.kapt) apply false

    //Navegacion segura
    alias(libs.plugins.navigation.safeargs) apply false
}