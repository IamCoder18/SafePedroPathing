plugins {
    id("java-library")
    id("io.deepmedia.tools.deployer")
    id("org.jetbrains.dokka")
}

dependencies {
    compileOnly(libs.annotations)
    dokkaPlugin(libs.dokka.java.plugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val dokkaJar = tasks.register<Jar>("dokkaJar") {
    dependsOn(tasks.named("dokkaGenerate"))
    from(dokka.basePublicationsDirectory.dir("html"))
    archiveClassifier = "html-docs"
}

deployer {
    projectInfo {
        name = "Safe Pedro Pathing Core"
        description = "A Synapse-safe fork of Pedro Pathing designed to work with the Synapse pub/sub library for FTC robot code."
        url = "https://github.com/IamCoder18/SafePedroPathing"
        scm {
            fromGithub("IamCoder18", "SafePedroPathing")
        }
        license("BSD 3-Clause License", "https://opensource.org/licenses/BSD-3-Clause")

        developer("Baron Henderson", "baron@pedropathing.com")
        developer("Havish Sripada", "havish@pedropathing.com")
    }

    content {
        component {
            fromJava()
            javaSources()
            docs(dokkaJar)
        }
    }

    if (System.getenv("PUBLISH_PEDRO") == "yes please") {
        signing {
            key = secret("MVN_GPG_KEY")
            password = secret("MVN_GPG_PASSWORD")
        }

        centralPortalSpec {
            auth {
                user = secret("SONATYPE_USERNAME")
                password = secret("SONATYPE_PASSWORD")
            }
            allowMavenCentralSync = false
        }

        nexusSpec("snapshot") {
            repositoryUrl = "https://central.sonatype.com/repository/maven-snapshots/"
            auth {
                user = secret("SONATYPE_USERNAME")
                password = secret("SONATYPE_PASSWORD")
            }
        }
    }

    localSpec()

    githubSpec {
        owner.set(findProperty("githubUser") as String? ?: "IamCoder18")
        repository.set("SafePedroPathing")
        auth {
            user.set(secret(findProperty("githubUser") as String? ?: "githubUser"))
            token.set(secret(findProperty("githubToken") as String? ?: "githubToken"))
        }
    }
}