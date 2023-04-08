plugins {
    kotlin("jvm") version "1.8.0"
}

group = "two.phases.commit"
repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("2pc")
    manifest {
        attributes(
            "Main-Class" to "MainKt"
        )
    }
}