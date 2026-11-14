plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.keepstamp.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.keepstamp.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"
    }

    signingConfigs {
        val fixedKeystorePath = System.getenv("FIXED_KEYSTORE_PATH")
        if (!fixedKeystorePath.isNullOrBlank() && file(fixedKeystorePath).exists()) {
            getByName("debug") {
                storeFile = file(fixedKeystorePath)
                storePassword = System.getenv("FIXED_KEYSTORE_PASS") ?: "android"
                keyAlias = System.getenv("FIXED_KEY_ALIAS") ?: "androiddebugkey"
                keyPassword = System.getenv("FIXED_KEY_PASS") ?: "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
