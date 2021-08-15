import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

  val compileKotlin: KotlinCompile by tasks
  compileKotlin.kotlinOptions.jvmTarget = "16"

  repositories {
    mavenCentral()
  }

  val implementation by configurations
  dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    implementation("io.netty:netty-all:4.1.66.Final")
  }
}
