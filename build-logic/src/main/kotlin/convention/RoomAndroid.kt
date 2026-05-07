package convention

import androidx.room.gradle.RoomExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureRoomAndroid() {
    pluginManager.apply("androidx.room")
    pluginManager.apply("com.google.devtools.ksp")

    val libs = extensions.libs

    extensions.configure<RoomExtension> {
        schemaDirectory("$projectDir/schemas")
    }

    dependencies {
        add("implementation", libs.findLibrary("androidx-room-runtime").get())
        add("implementation", libs.findLibrary("androidx-room-ktx").get())
        add("ksp", libs.findLibrary("androidx-room-compiler").get())
        add("testImplementation", libs.findLibrary("androidx-room-testing").get())
        add("testImplementation", libs.findLibrary("androidx-test-core").get())
        add("testImplementation", libs.findLibrary("robolectric").get())
    }
}
