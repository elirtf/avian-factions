import java.net.URI
import java.security.MessageDigest
import javax.inject.Inject

plugins {
    id("avian.java-conventions")
}

// --- FactionsUUID, built from pinned source (ADR-0007) ------------------------------------------
// Upstream publishes no Maven artifact and SpigotMC downloads are Cloudflare-gated, so the jar is
// built here from the release tag's source tarball. The tarball's SHA-256 is the pin. The one jar
// serves both as our compile-time API and as the plugin installed into run/plugins/.
abstract class BuildFactionsUuid @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Input abstract val version: Property<String>
    @get:Input abstract val sourceSha256: Property<String>
    @get:Internal abstract val javaHome: Property<String>
    @get:Internal abstract val workDir: DirectoryProperty
    @get:OutputFile abstract val jar: RegularFileProperty

    @TaskAction
    fun build() {
        val v = version.get()
        val dir = workDir.get().asFile.apply { deleteRecursively(); mkdirs() }
        val tarball = dir.resolve("UID-$v.tar.gz")
        URI.create("https://codeload.github.com/FactionsU/UID/tar.gz/refs/tags/$v").toURL()
            .openStream().use { input -> tarball.outputStream().use { input.copyTo(it) } }
        val digest = MessageDigest.getInstance("SHA-256").digest(tarball.readBytes())
            .joinToString("") { "%02x".format(it) }
        if (digest != sourceSha256.get()) {
            throw GradleException("FactionsUUID $v source: SHA-256 mismatch, expected ${sourceSha256.get()}, got $digest")
        }
        exec.exec { commandLine("tar", "xzf", tarball.absolutePath, "-C", dir.absolutePath) }
        val src = dir.resolve("UID-$v")
        // Upstream's :bukkit:jar and :bukkit:shadowJar write the same file, so a single shadowJar
        // run can ship the unshaded one (NoClassDefFoundError for cloud at enable). Running them
        // in this order, each excluding the others, always leaves the shaded jar in place.
        for (args in listOf(
            listOf(":bukkit:jar"),
            listOf(":bukkit:shadowJar", "-x", ":bukkit:jar"),
            listOf(":paper:shadowJar", "-x", ":bukkit:jar", "-x", ":bukkit:shadowJar"),
        )) {
            exec.exec {
                workingDir = src
                environment("JAVA_HOME", javaHome.get())
                commandLine(listOf("./gradlew", "--no-daemon", "-q") + args)
            }
        }
        src.resolve("paper/build/libs/factionsuuid.jar").copyTo(jar.get().asFile, overwrite = true)
        src.deleteRecursively()
        tarball.delete()
    }
}

val fuuidVersion = providers.gradleProperty("factionsUuidVersion")
val buildFactionsUuid = tasks.register<BuildFactionsUuid>("buildFactionsUuid") {
    description = "Downloads the pinned FactionsUUID source, verifies it and builds the plugin jar."
    group = "avian"
    version = fuuidVersion
    sourceSha256 = providers.gradleProperty("factionsUuidSourceSha256")
    javaHome = javaToolchains.launcherFor(java.toolchain).map { it.metadata.installationPath.asFile.absolutePath }
    workDir = layout.buildDirectory.dir("factionsuuid/work")
    jar = layout.buildDirectory.zip(fuuidVersion) { dir, v -> dir.file("factionsuuid/factionsuuid-$v.jar") }
}
val fuuidJar = buildFactionsUuid.flatMap { it.jar }

// Consumed by avian-plugin's downloadPlugins, which installs the jar into run/plugins/.
configurations.consumable("factionsUuidJar")
artifacts.add("factionsUuidJar", fuuidJar)

dependencies {
    api(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(libs.configurate.hocon)
    // FactionsUUID provides these classes at runtime; we only compile against them.
    compileOnly(files(fuuidJar))
    testImplementation(files(fuuidJar))
}
