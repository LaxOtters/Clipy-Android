package convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureHiltKotlin() {
    pluginManager.apply("com.google.devtools.ksp")

    val libs = extensions.libs

    dependencies {
        add("implementation", libs.findLibrary("hilt-core").get())
        add("ksp", libs.findLibrary("hilt-compiler").get())
    }
}
