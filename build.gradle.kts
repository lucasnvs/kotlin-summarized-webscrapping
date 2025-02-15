plugins {
    kotlin("jvm") version "2.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("mysql:mysql-connector-java:8.0.33")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}