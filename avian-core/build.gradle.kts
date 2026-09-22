plugins {
    id("avian.java-conventions")
}

dependencies {
    api(project(":avian-api"))
    implementation(libs.configurate.hocon)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.mariadb)   // referenced by class literal so shadow relocation follows it
    runtimeOnly(libs.flyway.mysql)

    "integrationTestImplementation"(project(":avian-testing"))
}
