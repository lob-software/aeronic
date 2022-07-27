plugins {
    java
}

group = "io.aeronic"
version = "0.0.16"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.0.1")
    implementation("com.google.auto.service:auto-service:1.0.1")
    implementation(Dependencies.aeron)
    implementation(Dependencies.awaitility)
    testImplementation("org.jooq:joor:0.9.14")
    testImplementation(project(":aeronic-tests"))
    testImplementation(Dependencies.assertJ)
    testImplementation(Dependencies.junitApi)
    testRuntimeOnly(Dependencies.junitEngine)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}