import convention.configureHiltAndroid
import convention.libs

plugins {
    id("clipy.android.library")
    id("clipy.android.compose")
}

configureHiltAndroid()

val libs = extensions.libs

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))

    implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())

    implementation(libs.findLibrary("androidx-navigation-compose").get())
    implementation(libs.findLibrary("hilt-navigation-compose").get())
}
