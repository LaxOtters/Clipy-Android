import convention.setNamespace

plugins {
    id("clipy.android.library")
    id("clipy.android.compose")
}

setNamespace("core.ui")

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(libs.junit)
}
