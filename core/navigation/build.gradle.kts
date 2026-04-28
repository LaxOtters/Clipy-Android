import convention.setNamespace

plugins {
    id("clipy.android.library")
    id("clipy.android.compose")
}

setNamespace("core.navigation")

dependencies {
    implementation(libs.androidx.navigation.compose)
}
