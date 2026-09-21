dependencyResolutionManagement {
    // Expose the root version catalog to convention plugins as `libs`.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
