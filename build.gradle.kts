plugins {
    id("java")
}

group = "net.superscary"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("org.jetbrains:annotations:26.1.0")
    runtimeOnly("org.jetbrains:annotations:26.1.0")

    implementation("org.hjson:hjson:3.1.0")
}

tasks.test {
    useJUnitPlatform()
}