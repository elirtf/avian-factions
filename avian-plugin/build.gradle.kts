import java.net.URI
import java.security.MessageDigest

plugins {
    id("avian.java-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

val mcVersion = providers.gradleProperty("mcVersion").get()
val paperBuild = providers.gradleProperty("paperBuild").get().toInt()

// FactionsUUID is built from pinned source in avian-factions (ADR-0007); this pulls that jar in.
val factionsUuid = configurations.dependencyScope("factionsUuid")
val factionsUuidJar = configurations.resolvable("factionsUuidJar") { extendsFrom(factionsUuid.get()) }

dependencies {
    implementation(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(project(":avian-combat"))
    implementation(project(":avian-economy"))
    implementation(project(":avian-factions"))
    implementation(project(":avian-ftop"))
    factionsUuid(project(":avian-factions", "factionsUuidJar"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString(), "mcVersion" to mcVersion)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.shadowJar {
    archiveBaseName = "AvianFactions"
    archiveClassifier = ""
    // Shadow 9.6 drops duplicate service files before merging unless duplicates are INCLUDEd.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()               // Flyway + JDBC driver register via META-INF/services
    // Everything third-party lives under one prefix so other plugins' copies can never collide.
    // Never relocate org.slf4j, net.kyori, com.google.gson, com.google.common: Paper provides them.
    val libs = "club.avian.factions.libs"
    relocate("org.spongepowered.configurate", "$libs.configurate")
    // VaultAPI is compileOnly: the Vault plugin provides it at runtime, so it is not shaded.
    relocate("io.leangen.geantyref", "$libs.geantyref")
    relocate("com.zaxxer.hikari", "$libs.hikari")
    relocate("org.mariadb.jdbc", "$libs.mariadb")
    relocate("org.flywaydb", "$libs.flyway")
    relocate("com.fasterxml.jackson", "$libs.jackson")
    relocate("org.jetbrains.annotations", "$libs.jetbrains")
    relocate("org.intellij.lang.annotations", "$libs.intellij")
    relocate("net.kyori.option", "$libs.kyori.option")   // Configurate dep; Paper ships its own via Adventure
    dependencies {
        exclude(dependency("org.slf4j:.*"))               // Paper provides slf4j-api
    }
    exclude("META-INF/maven/**")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.runServer {
    minecraftVersion(mcVersion)
    build(paperBuild)
    runDirectory(layout.projectDirectory.dir("../run").asFile)
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    // 8 GB: a large //paste buffers the whole schematic in memory. At 2 GB a spawn build pushed the
    // heap to 99.9% and the server spent ~85% of its time in full GC, frozen mid-paste.
    jvmArgs("-Xms2G", "-Xmx8G")
    // Running this task means the developer accepts the Minecraft EULA (https://aka.ms/MinecraftEULA)
    // for the local dev server only. Paper honours this property instead of eula.txt.
    systemProperty("com.mojang.eula.agree", "true")
}

// --- Third-party plugin stack --------------------------------------------------------------
// Pins and hashes from docs/research/mc-version-and-plugin-stack.md. `./gradlew downloadPlugins`
// fetches them into run/plugins/ so `runServer` boots the full stack (#12 verifies that boot).

data class PinnedPlugin(val file: String, val url: String, val algo: String, val hash: String)

val pluginStack = listOf(
    PinnedPlugin("EssentialsX-2.22.0.jar",
        "https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsX-2.22.0.jar",
        "SHA-256", "bda4685105977fca2e209820a9f0ad24275bd103390a03236f38e59bfdac58e6"),
    PinnedPlugin("EssentialsXSpawn-2.22.0.jar",
        "https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsXSpawn-2.22.0.jar",
        "SHA-256", "dd5377c4c921b9b67814209f4f6646ffbb959729003e721ec5e63c47c7c010b8"),
    PinnedPlugin("EssentialsXChat-2.22.0.jar",
        "https://github.com/EssentialsX/Essentials/releases/download/2.22.0/EssentialsXChat-2.22.0.jar",
        "SHA-256", "e5b0211f98af1eaba712d9294997639a39209db1fc842394a0923820073ec65a"),
    PinnedPlugin("LuckPerms-Bukkit-5.5.84.jar",
        "https://download.luckperms.net/1671/bukkit/loader/LuckPerms-Bukkit-5.5.84.jar",
        "SHA-256", "ee57b908b415a22a770f0e1f5af1e43de9cb2b81b33715c6c60d0f6d89ee3a58"),
    PinnedPlugin("Vault-1.7.3.jar",
        "https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar",
        "SHA-256", "a6b5ed97f43a5cf5bbaf00a7c8cd23c5afc9bd003f849875af8b36e6cf77d01d"),
    PinnedPlugin("worldedit-bukkit-7.4.5.jar",
        "https://cdn.modrinth.com/data/1u6JkXh5/versions/F5ea2ov3/worldedit-bukkit-7.4.5.jar",
        "SHA-256", "e5696a6d064b9969437a8888be91b0941148a28e0c3736de1554a00254a5d142"),
    PinnedPlugin("worldguard-bukkit-7.0.18.jar",
        "https://cdn.modrinth.com/data/DKY9btbd/versions/btHBavWa/worldguard-bukkit-7.0.18.jar",
        "SHA-256", "08f3ef58bc521c635d8c78aedaca96f151d2d397e7cd8018584955dd7468eb05"),
    PinnedPlugin("PlaceholderAPI-2.12.3.jar",
        "https://github.com/PlaceholderAPI/PlaceholderAPI/releases/download/2.12.3/PlaceholderAPI-2.12.3.jar",
        "SHA-256", "fde03259f5af6938f3c33eeb4d814000a1adabf1d2304ce14970be81f609a437"),
    PinnedPlugin("CoreProtect-CE-24.0.jar",
        "https://cdn.modrinth.com/data/Lu3KuzdV/versions/Kma0kBsY/CoreProtect-CE-24.0.jar",
        "SHA-256", "66cd362089bb8430e5a018ee77e9b433bf0dc9e65590d5f1a043a78d60415696"),
    PinnedPlugin("Chunky-Bukkit-1.5.3.jar",
        "https://cdn.modrinth.com/data/fALzjamp/versions/MdY6JATr/Chunky-Bukkit-1.5.3.jar",
        "SHA-256", "530d2c7430a96a39957391b7088be144daa3108f7665896d1c23aa8dd4af32f3"),
    // EconomyShopGUI 7.3.1, for the dev server. SpigotMC's versioned download is Cloudflare-gated so
    // this is spiget's "always latest" mirror and the hash is the pin. Upstream ships often, so
    // expect to re-pin: check the new jar, update the hash, commit. The *compile* dependency does
    // not come from here — that is the versioned EconomyShopGUI-API artifact on JitPack, so an
    // upstream plugin release can never break the build.
    PinnedPlugin("EconomyShopGUI-7.3.1.jar",
        "https://cdn.spiget.org/file/spiget-resources/69927.jar",
        "SHA-256", "51e19e014e1ea545d13f6094b554a072fedb93ae0681a45bffff27a8728bd869"),
    // RoseStacker 1.5.42 — mobs, items, blocks and spawners. Licence is MIT-Non-Distribution:
    // use, copy and modify are granted, redistribution and resale are not. Fine to run on our own
    // server; it could never be bundled into anything we hand out.
    PinnedPlugin("RoseStacker-1.5.42.jar",
        "https://cdn.modrinth.com/data/Bt25s2nb/versions/d0YeeOKd/RoseStacker-1.5.42.jar",
        "SHA-256", "b125525c64a0cdd1814d65de4f1fa360a05613e29998ceec5386a526fe064396"),
    // CrazyCrates, build named for our exact Minecraft version. MIT — the most permissive licence
    // in the stack. Reward tables stay free of Avian-specific prizes until the token and gem
    // commands exist; see docs/research/crates.md.
    PinnedPlugin("CrazyCrates-26.1.2-3726eba.jar",
        "https://cdn.modrinth.com/data/r3BBZyf3/versions/d4FEchgk/CrazyCrates-26.1.2-3726eba.jar",
        "SHA-256", "f7dcf465213c24451e7ff4b56c8047a33b424155a77dacc8a39e7757e8aa06d2"),
    // CommandTimer 8.18.0 (Apache-2.0): runs console commands on a weekly timetable. Switches
    // FactionsUUID grace on and off for the raid windows (ADR-0007), and later the event timetable.
    PinnedPlugin("CommandTimer-8.18.0.jar",
        "https://cdn.modrinth.com/data/UQTtLW4O/versions/nCJVp89f/commandtimer-java8%20%282%29.jar",
        "SHA-256", "d31b03f5ebf558469c0d44d26bf80c4d637dd5b83fe7d6e5a382fa726b4e9afa"),
)
// spark is bundled with Paper since 1.21; nothing to download.

tasks.register("downloadPlugins") {
    description = "Downloads the pinned third-party plugin stack into run/plugins/ and verifies hashes."
    group = "avian"
    val target = layout.projectDirectory.dir("../run/plugins")
    val stack = pluginStack
    outputs.dir(target)
    val builtFromSource: FileCollection = factionsUuidJar.get()
    inputs.files(builtFromSource)
    doLast {
        val dir = target.asFile.apply { mkdirs() }
        for (p in stack) {
            val out = dir.resolve(p.file)
            if (!out.exists()) {
                logger.lifecycle("Downloading ${p.file}")
                URI.create(p.url).toURL().openStream().use { input -> out.outputStream().use { input.copyTo(it) } }
            }
            val digest = MessageDigest.getInstance(p.algo).digest(out.readBytes())
                .joinToString("") { "%02x".format(it) }
            if (digest != p.hash) {
                out.delete()
                throw GradleException("${p.file}: ${p.algo} mismatch — expected ${p.hash}, got $digest. Deleted; re-run to retry.")
            }
            logger.lifecycle("  ok ${p.file} (${p.algo} ${digest.take(12)}…)")
        }
        // Built from source and hash-pinned at the tarball, so copied rather than downloaded.
        for (jar in builtFromSource) {
            jar.copyTo(dir.resolve(jar.name), overwrite = true)
            logger.lifecycle("  ok ${jar.name} (built from pinned source)")
        }
    }
}

// Copies the tracked third-party plugin config (dev-server/) into the git-ignored run/ directory.
// Without this a fresh clone boots the stack on defaults: SQLite/H2 storage and vanilla enchant caps.
val syncDevConfig = tasks.register<Copy>("syncDevConfig") {
    description = "Copies tracked dev-server config into run/."
    group = "avian"
    from(layout.projectDirectory.dir("../dev-server")) {
        exclude("README.md")
    }
    into(layout.projectDirectory.dir("../run"))
}

tasks.runServer {
    dependsOn(syncDevConfig)
}

// Prints the rank setup as console commands. LuckPerms has no "apply a file" command, so this
// exists to make the tracked script easy to paste in one go:
//   ./gradlew -q printRanks
// Then paste into the server console. The file itself is the source of truth, not the database.
tasks.register("printRanks") {
    description = "Prints dev-server/luckperms/ranks.lp as console commands, comments stripped."
    group = "avian"
    val script = layout.projectDirectory.file("../dev-server/luckperms/ranks.lp")
    doLast {
        script.asFile.readLines()
            .filterNot { it.isBlank() || it.trimStart().startsWith("#") }
            .forEach { println(it) }
    }
}
