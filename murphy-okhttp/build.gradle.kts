plugins {
    id("murphy.kotlin-library")
}

dependencies {
    api(project(":murphy-core"))

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
