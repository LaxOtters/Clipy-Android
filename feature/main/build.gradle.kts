import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.main")

dependencies {
    implementation(libs.androidx.activity.compose)
}
