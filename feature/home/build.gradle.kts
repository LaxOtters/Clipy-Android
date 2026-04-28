import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.home")

dependencies {
    implementation(libs.kotlinx.immutable)
}
