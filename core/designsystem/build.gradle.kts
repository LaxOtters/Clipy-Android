import convention.setNamespace

plugins {
    id("clipy.android.library")
    id("clipy.android.compose")
}

setNamespace("core.designsystem")

android {
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
