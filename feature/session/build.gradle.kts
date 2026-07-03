import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.session")

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
