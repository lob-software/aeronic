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
    implementation(Dependencies.aeron)
    implementation(Dependencies.awaitility)
    testImplementation("net.bytebuddy:byte-buddy:1.12.12")
    testImplementation("net.bytebuddy:byte-buddy-agent:1.12.12")
    testImplementation(Dependencies.junitApi)
    testRuntimeOnly(Dependencies.junitEngine)
    implementation(Dependencies.assertJ)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}