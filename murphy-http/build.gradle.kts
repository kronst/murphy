plugins {
    id("murphy.kotlin-library")
}

dependencies {
    api(project(":murphy-core"))

    testImplementation(libs.okhttp3.mockwebserver)
}
