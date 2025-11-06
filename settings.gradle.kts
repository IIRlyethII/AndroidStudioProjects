pluginManagement {
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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AirQualityMonitor"
include(":app")

// Core modules (funcionalidades compartidas)
include(":core:common")
include(":core:ui")

// Data layer (repositorios y fuentes de datos)
include(":data")

// Domain layer (lógica de negocio)
include(":domain")

// Feature modules (características específicas)
include(":feature:auth")
include(":feature:dashboard")
include(":feature:control")
include(":feature:monitoring")
