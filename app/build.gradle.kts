import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.tx24.android.application")
    id("com.tx24.android.application.compose")
    id("com.tx24.android.hilt")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tx24.spicyplayer"
    
    val keystorePropertiesFile = rootProject.file("app/keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.tx24.spicyplayer"

        versionCode = 5
        versionName = "v0.4.0-alpha-prerelease"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isEmpty) {
                // Fallback to debug if release props not found
                keyAlias = "androiddebugkey"
                keyPassword = "android"
                storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
                storePassword = "android"
            } else {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
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
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Spicy Player.d")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            val fileName = "SpicyPlayer-${variant.versionName}.apk"
            output.outputFileName = fileName
        }
    }
}


dependencies {
    implementation(libs.core.ktx)
    implementation(libs.media3.session)
    implementation(libs.media3.exoplayer)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.timber)
    
    // Extracted from features
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.tooling)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.gson.converter)
    implementation(libs.okio)
    implementation(libs.datastore)
    implementation(libs.coil)
    implementation(libs.androidx.palette)
    implementation(libs.material)
    implementation(libs.drag.reorder)
    implementation(libs.jaudio.tagger)
    implementation(libs.glance)
    implementation(libs.glance.material)

    api(libs.accompanist.permissions)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}