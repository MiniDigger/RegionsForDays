plugins {
  kotlin("jvm") version "1.5.20" apply false
  kotlin("plugin.serialization") version "1.5.20" apply false
}

group = "dev.benndorf"
version = "1.0.0-SNAPSHOT"


subprojects {
  apply {
    plugin("org.jetbrains.kotlin.jvm")
    plugin("org.jetbrains.kotlin.plugin.serialization")
  }

  repositories {
    mavenCentral()
  }

  val implementation by configurations
  dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.2.2")

    implementation("io.rsocket.kotlin:rsocket-core:0.13.1")
    implementation("io.rsocket.kotlin:rsocket-transport-local:0.13.1")
    implementation("io.ktor:ktor-server-cio:1.6.1")
    implementation("io.ktor:ktor-client-cio:1.6.1")
  }
}
