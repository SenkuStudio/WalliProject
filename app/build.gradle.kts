import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.legacy.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.walli.wallpaper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.walli.wallpaper"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        buildConfigField("String", "BASE_URL", "\"${localProperties.getProperty("BASE_URL") ?: ""}\"")
        buildConfigField("String", "API_KEY", "\"${localProperties.getProperty("API_KEY") ?: ""}\"")
        
        // Banner IDs
        buildConfigField("String", "ADMOB_BANNER_HOME_TOP", "\"${localProperties.getProperty("ADMOB_BANNER_HOME_TOP") ?: ""}\"")
        buildConfigField("String", "ADMOB_BANNER_HOME_BOTTOM", "\"${localProperties.getProperty("ADMOB_BANNER_HOME_BOTTOM") ?: ""}\"")
        buildConfigField("String", "ADMOB_BANNER_CATEGORIES", "\"${localProperties.getProperty("ADMOB_BANNER_CATEGORIES") ?: ""}\"")
        buildConfigField("String", "ADMOB_BANNER_CATEGORY_WALLPAPERS", "\"${localProperties.getProperty("ADMOB_BANNER_CATEGORY_WALLPAPERS") ?: ""}\"")
        buildConfigField("String", "ADMOB_BANNER_FAVORITES", "\"${localProperties.getProperty("ADMOB_BANNER_FAVORITES") ?: ""}\"")
        buildConfigField("String", "ADMOB_BANNER_SETTINGS", "\"${localProperties.getProperty("ADMOB_BANNER_SETTINGS") ?: ""}\"")

        // Interstitial IDs
        buildConfigField("String", "ADMOB_INTERSTITIAL_HOME", "\"${localProperties.getProperty("ADMOB_INTERSTITIAL_HOME") ?: ""}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_CATEGORY", "\"${localProperties.getProperty("ADMOB_INTERSTITIAL_CATEGORY") ?: ""}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_DOWNLOAD", "\"${localProperties.getProperty("ADMOB_INTERSTITIAL_DOWNLOAD") ?: ""}\"")

        // Rewarded IDs
        buildConfigField("String", "ADMOB_REWARDED_HOME", "\"${localProperties.getProperty("ADMOB_REWARDED_HOME") ?: ""}\"")
        buildConfigField("String", "ADMOB_REWARDED_CATEGORY", "\"${localProperties.getProperty("ADMOB_REWARDED_CATEGORY") ?: ""}\"")
        buildConfigField("String", "ADMOB_REWARDED_PREVIEW", "\"${localProperties.getProperty("ADMOB_REWARDED_PREVIEW") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    implementation(platform(libs.okhttp.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.runtime.livedata)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.compose.ui.test.junit4)

    implementation(libs.material)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.ads)
    implementation(libs.coroutines.android)
    implementation(libs.palette.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}
