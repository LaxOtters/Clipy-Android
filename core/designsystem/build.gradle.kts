import convention.setNamespace

plugins {
    id("clipy.android.library")
    id("clipy.android.compose")
}

setNamespace("core.designsystem")

dependencies {
    testImplementation(libs.junit)
}
