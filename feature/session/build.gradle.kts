import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.session")

dependencies {
    implementation(libs.androidx.navigation3.runtime)
}
