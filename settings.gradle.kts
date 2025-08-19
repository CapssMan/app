pluginManagement {
    repositories {
        google()  // Важно: должен быть первым
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:8.3.2")
            }
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion("1.9.20")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // <--- ЭТО ВАЖНО
        mavenCentral() // Стандартный репозиторий для многих библиотек
        maven { url = uri("https://jitpack.io") } // <--- ЭТО НУЖНО ДЛЯ ВАШЕЙ ЛИБЫ
    }
}

rootProject.name = "EconCalc"
include(":app")