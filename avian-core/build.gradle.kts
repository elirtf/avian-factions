plugins {
    id("avian.java-conventions")
}

dependencies {
    api(project(":avian-api"))
    implementation(libs.configurate.hocon)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.mysql)
    implementation(libs.mariadb)   // referenced by class literal so shadow relocation follows it
}
