import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow")
}

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
    implementation(project(":core"))
}

sourceSets {
    create("stubs") {
        java.setSrcDirs(listOf("src/stub/java"))
    }

    named("main") {
        compileClasspath += sourceSets["stubs"].output
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("compileStubsJava")
    classpath += sourceSets["stubs"].output
}

tasks.named<Jar>("jar") {
    from(sourceSets["main"].output)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("AutoModpackPlugin")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.named("assemble") {
    dependsOn("shadowJar")
}
