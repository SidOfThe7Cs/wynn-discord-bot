plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "sidly.discord_bot"
version = "1.2.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.6.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.google.code.gson:gson:2.13.1")
}

application {
    mainClass.set("sidly.discord_bot.MainEntrypoint")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    archiveClassifier.set("withDependencies")
    exclude("*.pom")
}

