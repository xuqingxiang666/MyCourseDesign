buildscript {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

subprojects {
    afterEvaluate {
        extensions.findByType<com.android.build.gradle.AppExtension>()?.apply {
            if (namespace == null) namespace = project.name
        }
        extensions.findByType<com.android.build.gradle.LibraryExtension>()?.apply {
            if (namespace == null) namespace = project.name
        }
    }
}