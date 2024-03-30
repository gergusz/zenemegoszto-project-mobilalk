// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    id("io.franzbecker.gradle-lombok") version "5.0.0"
    id("com.google.gms.google-services") version "4.4.1" apply false
}