plugins {
    `java-library`
    `jvm-test-suite`
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()
val javaVersion = providers.gradleProperty("javaVersion").get().toInt()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

dependencies {
    compileOnly(libs.paper.api)
    testImplementation(libs.paper.api)   // on the test runtime classpath too: JUnit reflects over Bukkit types
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.mockbukkit)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVersion
    // Prior-art lesson: version pain came from silently deprecated Bukkit API; make it loud.
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Werror"))
}

testing {
    suites {
        getByName<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit)
        }
        // ADR-0005 layer 3: repository tests against a real MariaDB via Testcontainers (needs Docker).
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
                implementation(platform(libs.junit.bom))
                implementation(libs.testcontainers.mariadb)
                implementation(libs.testcontainers.junit)
                implementation(libs.paper.api)
            }
            targets.all {
                testTask.configure {
                    shouldRunAfter(tasks.named("test"))
                    systemProperty("avian.mariadb.image", providers.gradleProperty("mariadbImage").get())
                }
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}
