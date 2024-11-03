import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group =
    "io.aeronic"
version =
    "0.0.1"

repositories {
    mavenCentral()
}


configurations {
    implementation {
        isCanBeResolved =
            true
    }
}

dependencies {
    annotationProcessor(
        project(
            ":aeronic-processor"
        )
    )
    testAnnotationProcessor(
        project(
            ":aeronic-processor"
        )
    )
    implementation(
        project(
            ":aeronic-processor"
        )
    )
    implementation(
        "tools.profiler:async-profiler:2.9"
    )
    implementation(
        Versions.aeron
    )
    implementation(
        Versions.awaitility
    )
    implementation(
        "org.hdrhistogram:HdrHistogram:2.1.12"
    )
}

tasks.getByName<Test>(
    "test"
) {
    useJUnitPlatform()
}

tasks {
    register<ShadowJar>(
        "benchmarkJar"
    ) {
        archiveFileName.set(
            "echo-benchmark.jar"
        )
        manifest {
            attributes["Main-Class"] =
                "io.aeronic.benchmarks.EchoBenchmark"
        }
        from(
            sourceSets.main.get().output
        )
        from(
            project.configurations.implementation
        )
    }
}