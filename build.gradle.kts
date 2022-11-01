plugins {
    java
    `java-library`
    `maven-publish`
    id("net.kyori.indra.git") // used for getting branch/commit info
    id("idea") // used to download sources and documentation
}

group = "me.konicai"
version = "1.0-SNAPSHOT"

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(8)
    }

    named("build") {
        dependsOn(named<Test>("test"))
    }

    getByName<Test>("test") {
        useJUnitPlatform()
    }

    processResources {
        expand("project_version" to project.version)
    }
}

dependencies {
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.22")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")

    annotationProcessor("org.projectlombok:lombok:1.18.22")
    compileOnly("org.projectlombok:lombok:1.18.22")

    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.1")
    compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
    api("cloud.commandframework:cloud-paper:1.7.1")
    api("cloud.commandframework:cloud-minecraft-extras:1.7.1")
    api("me.lucko:commodore:2.2")
    api("net.kyori:adventure-platform-bukkit:4.1.2")
}

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/") // geyser etc
    maven("https://oss.sonatype.org/content/repositories/snapshots") // bungeecord, spigot
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // spigot
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}