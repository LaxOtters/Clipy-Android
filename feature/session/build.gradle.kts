import convention.setNamespace

plugins {
    id("clipy.android.feature")
}

setNamespace("feature.session")

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.webkit)

    androidTestImplementation(libs.androidx.espresso.web)
    androidTestImplementation(libs.mockwebserver3)
    androidTestImplementation(libs.okhttp.tls)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
