import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.main")

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":feature:session"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
