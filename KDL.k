project {
    name = "Test Project"
    version = "1.0.0"

    entry {
        main = "main.Main"
    }

    dependencies {
        ["cloth.std", "1.0.0"]
        ["llvm", "18.0.0"]
    }

    build {
        target = "x86_64"
        optimize = 2
        debug = #true
        buildFlags = ""
    }
}