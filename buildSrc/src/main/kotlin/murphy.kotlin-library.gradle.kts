plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

repositories {
    mavenCentral()
}

dependencies {
    libs.findBundle("bom").get().get().forEach { lib ->
        implementation(platform(lib))
        testFixturesImplementation(platform(lib))
    }

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
