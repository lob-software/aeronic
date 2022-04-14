subprojects {
    tasks {
        withType<JavaCompile> {
            options.fork(mapOf(Pair("jvmArgs", listOf(
                    "--add-opens",
                    "java.base/sun.nio.ch=ALL-UNNAMED",
            ))))
        }

        withType<Test> {
            ignoreFailures = true // needed to continue tests after first failure occurs
            jvmArgs(listOf(
                    "--add-opens",
                    "java.base/sun.nio.ch=ALL-UNNAMED",
            ))
        }
    }
}
