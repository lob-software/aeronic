plugins {
    java
}

group = "io.aeronic"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testAnnotationProcessor(project(":aeronic-processor"))
    implementation(project(":aeronic-processor"))
    implementation("io.aeron:aeron-all:1.37.0")
    implementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    implementation("org.assertj:assertj-core:3.22.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}