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
    implementation("io.aeron:aeron-all:1.38.1")
    implementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("net.bytebuddy:byte-buddy:1.12.12")
    testImplementation("net.bytebuddy:byte-buddy-agent:1.12.12")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    implementation("org.assertj:assertj-core:3.23.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}