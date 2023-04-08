plugins {
  kotlin("jvm") version "1.8.0"
  id("io.ktor.plugin") version "2.2.4"
  application
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
application {
  mainClass.set("MainKt")
}

tasks.named<Jar>("jar") {
  archiveBaseName.set("2pc")
  manifest {
    attributes(
      "Main-Class" to "MainKt"
    )
  }
}

ktor {
  fatJar {
    archiveFileName.set("2pc-fat.jar")
  }
}