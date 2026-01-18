plugins {
    id("murphy.kotlin-library")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    api(project(":murphy-core"))

    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework:spring-webflux")
    compileOnly("io.projectreactor:reactor-core")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework:spring-test")
    testImplementation(libs.okhttp3.mockwebserver)
}
