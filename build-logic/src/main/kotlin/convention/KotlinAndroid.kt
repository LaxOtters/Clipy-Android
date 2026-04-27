package convention

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

private const val COMPILE_SDK = 36
private const val MIN_SDK = 24
private const val TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

internal fun Project.configureKotlinAndroid() {
    val libs = extensions.libs

    androidExtension.apply {
        compileSdk = COMPILE_SDK

        defaultConfig.apply {
            minSdk = MIN_SDK
            testInstrumentationRunner = TEST_RUNNER
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }
    }

    configureKotlinCompiler()

    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("android-desugar-jdk-libs").get())
    }
}
