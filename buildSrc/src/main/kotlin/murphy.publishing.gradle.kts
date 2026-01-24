plugins {
    `maven-publish`
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(project.name)
            description.set("Lightweight chaos engineering library for JVM to simulate network failures and latency in HTTP clients - ${project.name}")
            url.set("https://github.com/kronst/murphy")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }

            developers {
                developer {
                    id.set("kronst")
                    name.set("Roman Konstantynovskyi")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/kronst/murphy.git")
                developerConnection.set("scm:git:ssh://github.com/kronst/murphy.git")
                url.set("https://github.com/kronst/murphy")
            }
        }
    }

    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
