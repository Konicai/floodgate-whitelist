import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `java-library`
    `maven-publish`
    id("net.kyori.indra.git") // used for getting branch/commit info
    id("idea") // used to download sources and documentation
    id("com.github.johnrengelman.shadow")
}

group = "me.konicai"
version = "1.0"

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
    compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
    // todo: find a way to stop floodgate from providing guava.
    // tried excluding guava, disabling isTransitive, and setting older guava as a dependency with prefer 18.0/isForce

    implementation("cloud.commandframework:cloud-paper:1.7.1")
    implementation("cloud.commandframework:cloud-minecraft-extras:1.7.1")
    implementation("me.lucko:commodore:2.2")
    implementation("net.kyori:adventure-platform-bukkit:4.1.2")
    implementation("org.bstats:bstats-bukkit:3.0.0")
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net/") // brigadier
    maven("https://repo.opencollab.dev/main/") // geyser etc
    maven("https://oss.sonatype.org/content/repositories/snapshots") // bungeecord, spigot
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // spigot
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
}

tasks {
    getByName<Test>("test") {
        useJUnitPlatform()
    }

    named("build") {
        dependsOn(named("shadowJar"))
    }

    withType<ShadowJar> {
        val base = "me.konicai.floodgatewhitelist.shaded."

        dependencies {
            shadow {
                relocate("cloud.commandframework", base + "cloud")
                relocate("me.lucko.commodore", base + "commodore")
                relocate("net.kyori", base + "kyori")
                relocate("org.bstats", base + "bstats")
            }
        }

        archiveFileName.set("FloodgateWhitelist.jar")
    }
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
