import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.0"
  application
}

group = "two.phases.commit"
repositories {
  mavenCentral()
}

dependencies {
  implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.8.20")
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

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "17"
}

tasks.named<Jar>("jar") {
  archiveBaseName.set("2pc")
  manifest {
    attributes(
      "Main-Class" to "MainKt"
    )
  }
}