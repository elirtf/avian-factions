plugins {
    id("avian.java-conventions")
}

dependencies {
    api(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(libs.configurate.hocon)
    // FactionsUUID and RoseStacker are separate plugins that provide these classes at runtime.
    implementation(project(":avian-economy"))
    compileOnly(project(":avian-factions", "factionsUuidJar"))
    compileOnly(libs.rosestacker)
    testImplementation(project(":avian-factions", "factionsUuidJar"))
    testImplementation(libs.rosestacker)

    "integrationTestImplementation"(project(":avian-testing"))
}
