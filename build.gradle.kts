subprojects {
    tasks {
        // illegals-access is needed to access certain members via reflection when using JDK 16
        withType<JavaCompile> {
            options.fork(mapOf(Pair("jvmArgs", listOf(
                    "--add-opens",
                    "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                    "--illegal-access=permit"
            ))))
        }

        withType<Test> {
            ignoreFailures = true // needed to continue tests after first failure occurs
            jvmArgs(listOf(
                    "--add-opens",
                    "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                    "--illegal-access=permit"
            ))
        }
    }
}
