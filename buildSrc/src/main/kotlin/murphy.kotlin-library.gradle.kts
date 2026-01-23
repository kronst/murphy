plugins {
    kotlin("jvm")
    `maven-publish`
}

group = rootProject.group

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set("Chaos Engineering library for JVM - ${project.name}")
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
    }

    repositories {
        maven {
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
