import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.main")

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":feature:session"))

    implementation(libs.androidx.activity.compose)
}
