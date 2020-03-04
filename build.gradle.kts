import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "4.0.1"
    java
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    implementation("org.reflections:reflections:0.9.11")
    implementation("com.github.xaguzman:pathfinding:0.2.6")
    implementation("com.beust:klaxon:5.2")
//    implementation("com.github.trystan:AsciiPanel:ac179b1")
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
    archiveVersion.set(rootProject.version.toString().removeSuffix("-SNAPSHOT"))
    archiveClassifier.set("")
}
