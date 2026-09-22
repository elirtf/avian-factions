import java.net.URI
import java.security.MessageDigest

plugins {
    id("avian.java-conventions")
}

// EconomyShopGUI publishes no Maven artifact, so we fetch the jar we compile against ourselves.
// It is compileOnly and never shaded: the plugin provides these classes at runtime.
// Same pin as avian-plugin's downloadPlugins — SpigotMC's versioned URL is behind Cloudflare, so
// the hash is what pins the version, and an upstream change fails the build rather than sliding by.
val shopApiUrl = "https://cdn.spiget.org/file/spiget-resources/69927.jar"
val shopApiSha256 = "424fada37f0183836d339ed6afbd485b0bea799a1961dbbc37a500e0b8a93335"
val shopApiJar = layout.buildDirectory.file("api-libs/EconomyShopGUI-7.3.0.jar")

val downloadShopApi = tasks.register("downloadShopApi") {
    description = "Downloads the EconomyShopGUI jar we compile against."
    group = "avian"
    val target = shopApiJar
    val url = shopApiUrl
    val expected = shopApiSha256
    outputs.file(target)
    doLast {
        val file = target.get().asFile
        file.parentFile.mkdirs()
        if (!file.exists()) {
            URI.create(url).toURL().openStream().use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
        if (digest != expected) {
            file.delete()
            throw GradleException("EconomyShopGUI jar: SHA-256 mismatch — expected $expected, got $digest.")
        }
    }
}

dependencies {
    api(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(libs.configurate.hocon)
    compileOnly(libs.vault)
    testImplementation(libs.vault)
    compileOnly(files(shopApiJar))

    "integrationTestImplementation"(project(":avian-testing"))
}

tasks.compileJava {
    dependsOn(downloadShopApi)
}
