plugins {
    java
}

group =
    "io.aeronic"
version =
    VERSION

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(
        "com.google.auto.service:auto-service:1.0.1"
    )
    implementation(
        "com.google.auto.service:auto-service:1.0.1"
    )
    implementation(
        Versions.aeron
    )
    implementation(
        Versions.awaitility
    )
    testImplementation(
        "org.jooq:joor:0.9.14"
    )
    testImplementation(
        project(
            ":aeronic-tests"
        )
    )
    testImplementation(
        Versions.assertJ
    )
    testImplementation(
        Versions.junitApi
    )
    testRuntimeOnly(
        Versions.junitEngine
    )
}

tasks.getByName<Test>(
    "test"
) {
    useJUnitPlatform()
}