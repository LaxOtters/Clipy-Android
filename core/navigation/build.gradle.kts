import convention.setNamespace

plugins {
    id("clipy.android.library")
    id("clipy.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

setNamespace("core.navigation")

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
}
