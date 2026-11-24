plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nijatva.gallery"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.nijatva.gallery"
        minSdk = 29
        targetSdk = 35
        versionCode = 6
        versionName = "0.1.5"
        resValue("string", "app_name", "Nijatva Gallery")
    }

    // Debug builds use the standard debug keystore Gradle generates on each machine.
    // A shared debug key used to live in this repository; it was removed because a key
    // anyone can read lets someone sign an APK that installs over a debug install.
    // Release signing comes from the environment and is absent by default.
    signingConfigs {
        if (System.getenv("NIJATVA_KEYSTORE") != null) {
            create("release") {
                storeFile = file(System.getenv("NIJATVA_KEYSTORE"))
                storePassword = System.getenv("NIJATVA_STORE_PASSWORD")
                keyAlias = System.getenv("NIJATVA_KEY_ALIAS")
                keyPassword = System.getenv("NIJATVA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("NIJATVA_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
}
