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
    // AuraSkills provides these at runtime: farming XP for Harvester Hoe harvests.
    compileOnly(libs.auraskills.api)
    // The EconomyShopGUI plugin provides these classes at runtime; we only compile against them.
    compileOnly(libs.economyshopgui.api)
    // RoseStacker provides these at runtime: which mobs may stack (StackingRules).
    compileOnly(libs.rosestacker)
    testImplementation(libs.rosestacker)
    // EliteMobs provides these at runtime: rewards for elite and boss kills (EliteRewards).
    compileOnly(libs.elitemobs) { isTransitive = false }

    "integrationTestImplementation"(project(":avian-testing"))
}
