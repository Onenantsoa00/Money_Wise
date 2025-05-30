plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
    // Supprimez la ligne Python
    // id("com.chaquo.python") version "16.1.0" apply false
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
        // Supprimez la ligne Python
        // classpath("com.chaquo.python:gradle:16.1.0")
    }
}
