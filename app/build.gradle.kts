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

    // A debug-only key, committed on purpose: the debug APKs this project hands out keep
    // one signature, and a key that changes with every machine makes every install an
    // uninstall. It signs nothing that goes to a store, and its password is not a secret.
    //
    // It must never sign a release build. A key anyone can read cannot protect an update:
    // whoever holds this file can produce an APK that looks like a Nijatva Gallery update.
    // Release signing therefore comes from the environment, and is absent by default.
    signingConfigs {
        }
        if (System.getenv("NIJATVA_KEYSTORE") != null) {
            
            }
        }
    }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // No key in the environment means an unsigned release APK, which the suite's
            // index refuses to publish. That is the intended failure: silent, but not shipped.
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
