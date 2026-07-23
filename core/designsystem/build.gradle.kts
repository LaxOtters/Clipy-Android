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
}
