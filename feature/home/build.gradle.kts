import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.home")

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.immutable)
}
