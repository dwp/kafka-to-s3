rootProject.name = "kafka2s3"

buildCache {
    local<DirectoryBuildCache>{
        directory = File(settingsDir, "build-cache")
    }
}