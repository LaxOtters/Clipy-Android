plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(libs.gradle.android.plugin)
    implementation(libs.gradle.kotlin.plugin)
    implementation(libs.gradle.hilt.plugin)
    implementation(libs.gradle.ksp.plugin)
    implementation(libs.gradle.room.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    compileOnly(libs.gradle.compose.compiler.plugin)
}
