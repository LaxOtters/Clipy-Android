import convention.setNamespace

plugins {
    id("clipy.android.library")
    id("clipy.android.hilt")
}

setNamespace("data.session")

dependencies {
    implementation(project(":domain"))
    implementation(project(":data:storage"))
}
