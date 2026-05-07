pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Clipy"
include(":app")
include(":domain")

include(
    ":data:storage",
    ":data:session",
)

include(
    ":core:common",
    ":core:designsystem",
    ":core:navigation",
    ":core:ui",
)

include(
    ":feature:main",
    ":feature:home",
)
