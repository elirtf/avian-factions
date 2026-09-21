import java.net.URI
import java.security.MessageDigest

plugins {
    id("avian.java-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

val mcVersion = providers.gradleProperty("mcVersion").get()
val paperBuild = providers.gradleProperty("paperBuild").get().toInt()

dependencies {
    implementation(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(project(":avian-factions"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString(), "mcVersion" to mcVersion)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.shadowJar {
    archiveBaseName = "AvianFactions"
    archiveClassifier = ""
    mergeServiceFiles()
    // Everything third-party lives under one prefix so other plugins' copies can never collide.
    // Relocations for Configurate/Hikari/Flyway/driver are added as those dependencies land (#10).
    // Never relocate org.slf4j, net.kyori, com.google.gson, com.google.common: Paper provides them.
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.runServer {
    minecraftVersion(mcVersion)
    build(paperBuild)
    runDirectory(layout.projectDirectory.dir("../run").asFile)
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    jvmArgs("-Xmx2G")
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
)
// spark is bundled with Paper since 1.21; nothing to download.

tasks.register("downloadPlugins") {
    description = "Downloads the pinned third-party plugin stack into run/plugins/ and verifies hashes."
    group = "avian"
    val target = layout.projectDirectory.dir("../run/plugins")
    val stack = pluginStack
    outputs.dir(target)
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
    }
}
