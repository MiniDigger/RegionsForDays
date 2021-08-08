plugins {
  kotlin("jvm") version "1.5.10" apply false
}

group = "dev.benndorf"
version = "1.0.0-SNAPSHOT"


subprojects {
  apply {
    plugin("org.jetbrains.kotlin.jvm")
  }

  repositories {
    mavenCentral()
  }

  val implementation by configurations
  dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
  }
}
