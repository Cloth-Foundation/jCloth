project {
    name "Test Project"
    version "0.1.0"
    description "A test project for INI configuration"

    author {
        name "John Doe"
        email "john.doe@example.com"
    }
}

dependencies {
    LibraryA "1.0.0"
    LibraryB "2.0.0"
}

build {
    target "x86_64-unknown-linux-gnu"
    outputDir "dist"
    type "release"

    options {
        optimize true
        debug false
    }

    buildFlags {
        flag "jb"
    }
}