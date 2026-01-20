plugins {
    id("murphy.kotlin-library")
}

dependencies {
    api(project(":murphy-core"))

    implementation(libs.ktor.client.core)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.cio)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}
