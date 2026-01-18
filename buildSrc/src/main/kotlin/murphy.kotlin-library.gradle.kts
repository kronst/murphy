plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.findLibrary("junit-bom").get()))
    testFixturesImplementation(platform(libs.findLibrary("junit-bom").get()))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
