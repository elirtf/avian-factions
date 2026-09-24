plugins {
    id("avian.java-conventions")
}

dependencies {
    api(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(libs.configurate.hocon)
    compileOnly(libs.vault)
    testImplementation(libs.vault)
    // PlaceholderAPI provides these at runtime: our chat placeholders (%avian_balance% and friends).
    compileOnly(libs.placeholderapi)
    testImplementation(libs.placeholderapi)
    // The EconomyShopGUI plugin provides these classes at runtime; we only compile against them.
    compileOnly(libs.economyshopgui.api)

    "integrationTestImplementation"(project(":avian-testing"))
}
