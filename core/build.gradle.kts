plugins {
    id("java-library")
    id("io.deepmedia.tools.deployer")
    id("org.jetbrains.dokka")
    id("com.diffplug.spotless")
    `maven-publish`
}

dependencies {
    dokkaPlugin(libs.dokka.java.plugin)
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("com.google.truth:truth:1.4.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.test {
    useJUnitPlatform()
}

val dokkaJar =
    tasks.register<Jar>("dokkaJar") {
        description = "Generates a Dokka Jar"
        dependsOn(tasks.named("dokkaGenerate"))
        from(dokka.basePublicationsDirectory.dir("html"))
        archiveClassifier = "html-docs"
    }

publishing {
    publications {
        create<MavenPublication>("github") {
            from(components["java"])
            pom {
                name.set("Safe Pedro Pathing Core")
                description.set(
                    "A Synapse-safe fork of Pedro Pathing designed to work with the Synapse pub/sub library for FTC robot code.",
                )
                url.set("https://github.com/IamCoder18/SafePedroPathing")
                licenses {
                    license {
                        name.set("BSD 3-Clause License")
                        url.set("https://opensource.org/licenses/BSD-3-Clause")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/IamCoder18/SafePedroPathing.git")
                    developerConnection.set("scm:git:ssh://git@github.com:IamCoder18/SafePedroPathing.git")
                    url.set("https://github.com/IamCoder18/SafePedroPathing")
                }
                developers {
                    developer {
                        id.set("IamCoder18")
                        name.set("IamCoder18")
                    }
                    developer {
                        id.set("Baron Henderson")
                    }
                    developer {
                        id.set("Havish Sripada")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/IamCoder18/SafePedroPathing")
            credentials {
                username = (project.findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_USER") ?: System.getenv("GITHUB_ACTOR")
                password = (project.findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
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
        developer("Davis Luxenberg", "davis@pedropathing.com")
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
            allowMavenCentralSync = true
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
}

spotless {
    java {
        target("src/**/*.java")

        palantirJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()

        licenseHeaderFile(rootProject.file("notice.txt"))
    }

    kotlinGradle {
        ktlint("1.2.1")
        target("*.gradle.kts")
    }

    format("misc") {
        target("*.md", "*.yaml", "*.yml", "*.json", ".gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
