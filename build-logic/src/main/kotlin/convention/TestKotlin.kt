package convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureTest() {
    val libs = extensions.libs

    dependencies {
        add("testImplementation", libs.findLibrary("junit").get())
    }
}
