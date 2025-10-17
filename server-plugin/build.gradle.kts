import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy

plugins {
    java
}

group = providers.gradleProperty("mod.group").get()
version = providers.gradleProperty("mod_version").get()
description = "Bridge plugin that exposes AutoModpack hosting to Paper/Spigot servers"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.papermc.io/repository/maven-snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.82")
    implementation("org.tomlj:tomlj:1.1.1")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5.1")
    implementation("io.netty:netty-all:4.1.118.Final")
    implementation("com.github.luben:zstd-jni:1.5.7-5")
}

sourceSets {
    create("stubs") {
        java.setSrcDirs(listOf("src/stub/java"))
    }

    named("main") {
        compileClasspath += sourceSets["stubs"].output
    }

    named("test") {
        compileClasspath += sourceSets["stubs"].output
        runtimeClasspath += sourceSets["stubs"].output
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("compileStubsJava")
    classpath += sourceSets["stubs"].output
}

tasks.named<JavaCompile>("compileTestJava") {
    dependsOn("compileStubsJava")
    classpath += sourceSets["stubs"].output
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Test>().configureEach {
    enabled = false
}

val pluginBaseName = "AutoModpackPlugin"

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(pluginBaseName)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set(pluginBaseName)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) })
}

tasks.processResources {
    val props = mapOf(
        "pluginVersion" to project.version.toString(),
        "automodpackVersion" to project.version.toString()
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml", "config.yml")) {
        expand(props)
    }
}

val pluginDistribution = tasks.register<Copy>("pluginDistribution") {
    dependsOn(tasks.named<Jar>("jar"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("distributions/AutoModpackPlugin"))
}

tasks.named("assemble") {
    dependsOn(pluginDistribution)
}

tasks.named("build") {
    dependsOn(pluginDistribution)
}
