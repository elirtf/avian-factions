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
    // Chat formatting with hover tooltips (replaces EssentialsXChat, which cannot do hover).
    PinnedPlugin("carbonchat-paper-3.0.0-beta.39.jar",
        "https://github.com/Hexaoxide/Carbon/releases/download/v3.0.0-beta.39/carbonchat-paper-3.0.0-beta.39.jar",
        "SHA-256", "bc79bba5b67f4ceeca531775970ce6e2922ddff87999e12e463cdffa8e6fe4f5"),
    PinnedPlugin("LuckPerms-Bukkit-5.5.84.jar",
        "https://download.luckperms.net/1671/bukkit/loader/LuckPerms-Bukkit-5.5.84.jar",
        "SHA-256", "ee57b908b415a22a770f0e1f5af1e43de9cb2b81b33715c6c60d0f6d89ee3a58"),
    PinnedPlugin("Vault-1.7.3.jar",
        "https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar",
        "SHA-256", "a6b5ed97f43a5cf5bbaf00a7c8cd23c5afc9bd003f849875af8b36e6cf77d01d"),
    // FastAsyncWorldEdit replaces WorldEdit. Plain WorldEdit pastes on the main thread and holds the
    // whole paste plus an undo copy in memory: the 46-million-block spawn schematic froze the server
    // twice. FAWE pastes in chunks off the main thread. Its plugin.yml `provides: [ WorldEdit ]`, so
    // WorldGuard, CoreProtect and our softdepend on WorldEdit all keep working unchanged.
    //
    // A DEV BUILD, deliberately. Release 2.15.4 (Modrinth tags it 26.1.2) fails on our server with
    // ExceptionInInitializerError in BlockTypesCache — its block registry cannot load, so pasting
    // cannot work. Upstream fixed exactly that in #3641 (2026-09-10, "Bind BlockTypes constants to
    // explicit ids"), after 2.15.4. Jenkins build 1389 = commit 944c416 includes it. Move back to a
    // release (2.15.5+) once one ships. Build-number URLs are stable, but Jenkins can prune old
    // builds eventually — if this 404s, take the newest successful build and re-pin the hash.
    PinnedPlugin("FastAsyncWorldEdit-Paper-2.15.5-SNAPSHOT-1389.jar",
        "https://ci.athion.net/job/FastAsyncWorldEdit/1389/artifact/artifacts/FastAsyncWorldEdit-Paper-2.15.5-SNAPSHOT.jar",
        "SHA-256", "f234b5c9617541d92b221d80d72c690f39c9391e4afa1b0d226f5fc9df6840de"),
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
    // AuraSkills 2.4.0 (GPL-3.0): skills, stats and abilities — the RPG grind. Modrinth tags it
    // 26.1–26.3 but not 26.1.2; boot-tested clean on 26.1.2 anyway. Combat stats are capped in
    // dev-server/plugins/AuraSkills; see docs/research/skills-rpg.md.
    PinnedPlugin("AuraSkills-2.4.0.jar",
        "https://cdn.modrinth.com/data/uDdZAVls/versions/9rSJ3THD/AuraSkills-2.4.0.jar",
        "SHA-256", "de54cbd2e33d65e8b1704751ae4121ed2f5b466ba89c63a1aadf3a3e629d6a40"),
    // Bedrock crossplay: Geyser translates Bedrock clients (UDP 19132) into Java ones, Floodgate
    // lets them in without a Java account. Geyser only speaks the newest Java protocol (26.2), so
    // ViaVersion lets it — and 26.2 Java clients — join our 26.1.2 server. GeyserMC publishes every
    // build as "latest"; the build-number URLs are stable and the hash is the pin. All three
    // boot-tested clean on Paper 26.1.2 build 74; see docs/research/bedrock-crossplay.md.
    PinnedPlugin("ViaVersion-5.12.0.jar",
        "https://cdn.modrinth.com/data/P1OZGk5p/versions/FaishMnD/ViaVersion-5.12.0.jar",
        "SHA-256", "c4d512fa9760fa41d17abaedde12aa1f4c9bde920d0a992fe0fc016962f126be"),
    PinnedPlugin("Geyser-Spigot-2.11.3-b1246.jar",
        "https://download.geysermc.org/v2/projects/geyser/versions/2.11.3/builds/1246/downloads/spigot",
        "SHA-256", "d1607770723a740b4165afed56bcafe544ea2767d71eeda9867f985031812600"),
    PinnedPlugin("floodgate-spigot-2.2.5-b141.jar",
        "https://download.geysermc.org/v2/projects/floodgate/versions/2.2.5/builds/141/downloads/spigot",
        "SHA-256", "21570aff9ce17d6983928e8552777760e1ede5050026b04c686b0ae112e6fd7e"),
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
        // Remove any jar that is no longer pinned. Without this, swapping a plugin (WorldEdit →
        // FastAsyncWorldEdit) leaves the old jar behind and both load, fighting over the same API.
        // Our own AvianFactions jar is not in run/plugins — run-paper adds it with -add-plugin.
        val expected = stack.map { it.file }.toSet() + builtFromSource.map { it.name }.toSet()
        dir.listFiles { f -> f.isFile && f.name.endsWith(".jar") && f.name !in expected }
            ?.forEach { stale ->
                stale.delete()
                logger.lifecycle("  removed ${stale.name} (no longer pinned)")
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
