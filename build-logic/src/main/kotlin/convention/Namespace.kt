package convention

import org.gradle.api.Project

private const val BASE_NAMESPACE = "com.laxotters.clipy"

internal fun Project.setNamespace(name: String) {
    androidExtension.namespace = if (name.isBlank()) {
        BASE_NAMESPACE
    } else {
        "$BASE_NAMESPACE.$name"
    }
}
