plugins {
    id("avian.java-conventions")
}

dependencies {
    api(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(libs.configurate.hocon)
    compileOnly(libs.vault)
    testImplementation(libs.vault)
    // The EconomyShopGUI plugin provides these classes at runtime; we only compile against them.
    compileOnly(libs.economyshopgui.api)

    "integrationTestImplementation"(project(":avian-testing"))
}
