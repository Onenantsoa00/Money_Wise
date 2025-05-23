plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
    // Ajoutez ceci si vous utilisez KSP
    // id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
}

// Ajoutez cette section si elle n'existe pas
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    }
}