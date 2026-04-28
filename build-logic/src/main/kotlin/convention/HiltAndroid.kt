package convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureHiltAndroid() {
    pluginManager.apply("com.google.dagger.hilt.android")
    pluginManager.apply("com.google.devtools.ksp")

    val libs = extensions.libs

    dependencies {
        add("implementation", libs.findLibrary("hilt-android").get())
        add("ksp", libs.findLibrary("hilt-android-compiler").get())
        add("kspAndroidTest", libs.findLibrary("hilt-android-compiler").get())
    }
}
