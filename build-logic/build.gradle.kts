plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(libs.gradle.android.plugin)
    implementation(libs.gradle.kotlin.plugin)
    compileOnly(libs.gradle.compose.compiler.plugin)
}
