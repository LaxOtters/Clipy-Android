import convention.setNamespace

plugins {
    id("clipy.android.library")
    id("clipy.android.hilt")
    id("clipy.android.room")
}

setNamespace("data.storage")

dependencies {
    implementation(libs.androidx.datastore.preferences)
}
