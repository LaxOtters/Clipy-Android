plugins {
    id("clipy.android.application")
    id("clipy.android.hilt")
}

android {
    namespace = "com.laxotters.clipy"

    defaultConfig {
        applicationId = "com.laxotters.clipy"
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
        }

        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":feature:main"))
}
