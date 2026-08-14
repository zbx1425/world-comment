plugins {
    id("net.neoforged.moddev") version "2.0.140"
    id("neoforge-mutex")
    `maven-publish`
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.name") as String}-neoforge"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven("https://maven.isxander.dev/releases") { name = "Xander Maven" }
}

dependencies {
    implementation("dev.isxander:yet-another-config-lib:${property("deps.yacl")}")

    jarJar(implementation("io.lettuce:lettuce-core:6.2.3.RELEASE") { version { prefer("6.2.3") } })
    jarJar(implementation("io.projectreactor:reactor-core:3.4.27") { version { prefer("3.4.27") } })
    jarJar(implementation("org.reactivestreams:reactive-streams:1.0.4") { version { prefer("1.0.4") } })
//    if (sc.current.parsed < "1.21.9") additionalRuntimeClasspath("io.lettuce:lettuce-core:7.6.0.RELEASE")

    jarJar(implementation("dev.matrixlab.webp4j:webp4j-core:2.1.0") {
        version {
            prefer("2.1.0")
        }
    })
//    if (sc.current.parsed < "1.21.9") additionalRuntimeClasspath("dev.matrixlab.webp4j:webp4j-core:2.5.0")
}

neoForge {
    version = property("deps.neo_loader") as String

    mods {
        register("worldcomment") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = file("../../run/")
            client()
        }

        register("server") {
            gameDirectory = file("../../run/")
            server()
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

publishing {
    publications {
        register<MavenPublication>("mod") {
            from(components["java"])

            groupId = property("mod.group") as String
            artifactId = base.archivesName.get().lowercase()
            version = project.version as String

            pom {
                name = property("mod.name") as String
                description = "Place comments in your Minecraft world"
                url = "https://github.com/zbx1425/WorldComment"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://github.com/zbx1425/WorldComment/blob/master/LICENSE"
                    }
                }
                developers {
                    developer {
                        id = "zbx1425"
                        name = "Zbx1425"
                        email = "support@zbx1425.cn"
                    }
                }
                scm {
                    url = "https://github.com/zbx1425/WorldComment"
                    connection = "scm:git:git//github.com/zbx1425/WorldComment.git"
                    developerConnection = "scm:git:ssh://git@github.com/zbx1425/WorldComment.git"
                }
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
        }

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        exclude("fabric.mod.json", "*.ct", "*.classtweaker")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
