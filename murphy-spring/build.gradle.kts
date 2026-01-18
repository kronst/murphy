plugins {
    id("murphy.kotlin-library")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    api(project(":murphy-core"))

    compileOnly("org.springframework:spring-web")

    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-test")
}
