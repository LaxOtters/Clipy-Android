package convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCoroutineAndroid() {
    val libs = extensions.libs

    dependencies {
        add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
    }
}

internal fun Project.configureCoroutineKotlin() {
    val libs = extensions.libs

    dependencies {
        add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
    }
}
