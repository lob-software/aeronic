plugins {
    id("checkstyle")
}

subprojects {
    apply(
        plugin = "checkstyle"
    )
    checkstyle {
        toolVersion =
            "10.0"
    }

    tasks {
        withType<Checkstyle> {
            reports {
                html.required.set(
                    true
                )
                xml.required.set(
                    false
                )
            }
        }

        register(
            "checkstyle"
        ) {
            dependsOn(
                "checkstyleMain",
                "checkstyleTest"
            )
        }

        withType<JavaCompile> {
            options.fork(
                mapOf(
                    Pair(
                        "jvmArgs",
                        listOf(
                            "--add-opens",
                            "java.base/sun.nio.ch=ALL-UNNAMED",
                        )
                    )
                )
            )
        }

        withType<JavaExec> {
            jvmArgs(
                listOf(
                    "--add-opens",
                    "java.base/sun.nio.ch=ALL-UNNAMED",
                )
            )
        }

        withType<Test> {
            ignoreFailures =
                true // needed to continue tests after first failure occurs
            jvmArgs(
                listOf(
                    "--add-opens",
                    "java.base/sun.nio.ch=ALL-UNNAMED",
                )
            )
        }
    }
}