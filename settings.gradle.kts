plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "murphy"

include("murphy-core")
include("murphy-okhttp")
