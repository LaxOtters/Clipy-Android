import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.session")

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)

    androidTestImplementation(libs.androidx.espresso.web)
    androidTestImplementation(libs.mockwebserver3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
