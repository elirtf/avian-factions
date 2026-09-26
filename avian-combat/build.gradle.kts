plugins {
    id("avian.java-conventions")
}

dependencies {
    api(project(":avian-api"))
    implementation(project(":avian-core"))
    implementation(libs.configurate.hocon)
    // RoseStacker provides these at runtime: which dying mob still has a stack behind it.
    compileOnly(libs.rosestacker)

    "integrationTestImplementation"(project(":avian-testing"))
}
