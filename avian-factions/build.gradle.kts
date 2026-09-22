plugins {
    id("avian.java-conventions")
}

dependencies {
    api(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(libs.configurate.hocon)

    // Shares avian-core's one-container-per-JVM MariaDB fixture, so both suites reuse a container
    // and every module's migrations are applied to it.
    "integrationTestImplementation"(project(":avian-testing"))
}
