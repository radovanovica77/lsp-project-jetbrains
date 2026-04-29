import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.23"
    antlr
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.logolsp"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")
    implementation(kotlin("stdlib"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("com.logolsp.MainKt")
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf(
        "-package", "com.logolsp.parser",
        "-visitor",
        "-no-listener"
    )
    outputDirectory = file(
        "${layout.buildDirectory.get()}/generated-src/antlr/main/com/logolsp/parser"
    )
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("logo-lsp")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "com.logolsp.MainKt"
    }
}