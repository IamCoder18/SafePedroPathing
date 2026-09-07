pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pedropathing.com/")
        maven("https://maven.pkg.github.com/IamCoder18/synapse") {
            credentials {
                val ghUser = providers.gradleProperty("githubUser").orElse(providers.environmentVariable("GITHUB_USER")).orElse(providers.environmentVariable("GITHUB_ACTOR"))
                val ghToken = providers.gradleProperty("githubToken").orElse(providers.environmentVariable("GITHUB_TOKEN"))
                if (ghUser.isPresent && ghToken.isPresent) {
                    setUsername(ghUser.get())
                    setPassword(ghToken.get())
                }
            }
        }
    }
}

rootProject.name = "Pedro Pathing"
include(":core")
include(":ftc")
