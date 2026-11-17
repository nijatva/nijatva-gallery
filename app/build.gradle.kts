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

    // A debug-only key, committed on purpose: the APKs this project hands out are debug
    // builds, and a key that changes with every machine makes every install an uninstall.
    // It signs nothing that goes to a store, and the passwords are not secrets.
    signingConfigs {
        create("nijatva") {
            storeFile = rootProject.file("keystore/nijatva-debug.jks")
            storePassword = "nijatva"
            keyAlias = "nijatva"
            keyPassword = "nijatva"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("nijatva")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("nijatva")
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
