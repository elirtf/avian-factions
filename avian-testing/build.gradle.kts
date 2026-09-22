plugins {
    id("avian.java-conventions")
}

// Test-only helpers shared by every module's integrationTest suite (ADR-0005). Not shaded into
// the plugin jar — avian-plugin does not depend on it.
dependencies {
    api(project(":avian-core"))
    api(libs.testcontainers.mariadb)
    api(libs.junit.jupiter)
    api(platform(libs.junit.bom))
}
