import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
//    id("kotlinx-serialization") version "1.3.31"
    id("com.github.johnrengelman.shadow") version "4.0.1"
    idea
}

group = "vladyagl"
version = "0.2-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(kotlin("test"))
//    implementation(kotlin("serialization"))
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")
    implementation("org.reflections:reflections:0.9.11")
    implementation("com.github.xaguzman:pathfinding:0.2.6")
    implementation("com.beust:klaxon:5.0.5")
    implementation("com.github.trystan:AsciiPanel:ac179b1")
    implementation(files("lib/rlforj.0.2.jar"))
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to "gehenna.MainKt")
    }
}

tasks.withType<ShadowJar> {
    classifier = ""
    version = rootProject.version.toString().removeSuffix("-SNAPSHOT")
}
