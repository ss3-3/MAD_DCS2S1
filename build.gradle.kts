// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.3" apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}

buildscript {
    repositories {
        google()
        mavenCentral();
        maven { url = uri("https://maven.google.com") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.11.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
        classpath("com.google.gms:google-services:4.4.0")

    }

}