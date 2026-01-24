plugins {
    `java-platform`
    id("murphy.publishing")
}

group = rootProject.group

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":murphy-core"))
        api(project(":murphy-http"))
        api(project(":murphy-ktor"))
        api(project(":murphy-okhttp"))
        api(project(":murphy-spring"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])
        }
    }
}
