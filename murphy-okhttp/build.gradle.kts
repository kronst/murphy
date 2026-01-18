plugins {
    id("murphy.kotlin-library")
}

dependencies {
    api(project(":murphy-core"))

    implementation(libs.okhttp3.client)

    testImplementation(libs.bundles.okhttp3)
}
