plugins {
    java
}

group = "io.aeronic"
version = "0.0.6"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")
    implementation("com.google.auto.service:auto-service:1.0.1")
    implementation("io.aeron:aeron-all:1.37.0")
}
