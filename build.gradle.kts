plugins {
    alias(libs.plugins.quilt.loom)
    `maven-publish`
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

base {
    archivesName.set(providers.gradleProperty("archives_base_name"))
}

repositories {
    maven("https://maven.quiltmc.org/repository/release") {
        name = "Quilt"
    }
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.quilt.loader)
    implementation(libs.fabric.api)
}

loom {
    mods {
        create("timed_end_unlock") {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        configureEach {
            vmArg("-Dloader.disableEnvironmentStrip=true")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
    options.release.set(25)
}

tasks.processResources {
    val expandProps = mapOf(
        "version" to project.version,
        "group" to project.group,
        "minecraft_version" to libs.versions.minecraft.get(),
        "loader_version" to libs.versions.quilt.loader.get()
    )
    inputs.properties(expandProps)
    filteringCharset = "UTF-8"

    filesMatching("quilt.mod.json") {
        expand(expandProps)
    }
}
