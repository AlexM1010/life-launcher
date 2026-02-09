pluginManagement {
    repositories {
        google()
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

rootProject.name = "LifeLauncher"
include(":app")

// Include life-widgets from sibling folder (hub-and-spoke architecture)
includeBuild("../life-widgets") {
    dependencySubstitution {
        substitute(module("app.lifelauncher:widgets")).using(project(":"))
    }
}
