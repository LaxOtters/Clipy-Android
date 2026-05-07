import convention.setNamespace

plugins {
    id("clipy.android.library")
    id("clipy.android.hilt")
}

setNamespace("data.session")

dependencies {
    implementation(project(":domain"))
    implementation(project(":data:storage"))
    implementation(libs.androidx.room.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
