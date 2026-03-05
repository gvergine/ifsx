plugins {
    application
}

application {
    mainClass.set("io.github.gvergine.ifsx.cli.Main")
}

dependencies {
    implementation(project(":ifsx-core"))
    implementation("info.picocli:picocli:${property("picocliVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
}

// Minimal JRE for the CLI: excludes java.desktop (and therefore libasound2/libX11/etc.)
// jdk.unsupported is needed by picocli (sun.misc.Unsafe reflection).
tasks.register<Exec>("jlinkCliRuntime") {
    val jreDir = layout.buildDirectory.dir("jre-cli")
    doFirst { jreDir.get().asFile.deleteRecursively() }
    commandLine = listOf(
        "jlink",
        "--add-modules", "java.base,java.logging,jdk.unsupported",
        "--output", jreDir.get().asFile.absolutePath,
        "--strip-debug",
        "--compress", "2",
        "--no-header-files",
        "--no-man-pages"
    )
}

val platform = if (System.getProperty("os.name").lowercase().contains("win")) "win" else "linux"

tasks.named<Zip>("distZip") {
    archiveBaseName.set("${project.name}-$platform")
}

// Native installer via jpackage
tasks.register<Exec>("jpackage") {
    dependsOn("jlinkCliRuntime", "installDist")
    val installDir = layout.buildDirectory.dir("install/ifsx-cli")
    val outputDir  = layout.buildDirectory.dir("jpackage")
    val jreDir     = layout.buildDirectory.dir("jre-cli")
    val jpackageRes = file("src/jpackage")
    val appVersion = project.version.toString()

    doFirst {
        outputDir.get().asFile.mkdirs()
    }

    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val isLinux   = System.getProperty("os.name").lowercase().contains("linux")

    commandLine = buildList {
        add("jpackage")
        add("--type"); add(if (isWindows) "msi" else "deb")
        add("--name"); add("ifsx")
        add("--app-version"); add(appVersion)
        add("--vendor"); add("Giovanni Vergine")
        add("--description"); add("IFS Extract/Repack Tool for QNX images")
        add("--input"); add(installDir.get().dir("lib").asFile.absolutePath)
        add("--main-jar"); add("ifsx-cli-${appVersion}.jar")
        add("--main-class"); add("io.github.gvergine.ifsx.cli.Main")
        add("--runtime-image"); add(jreDir.get().asFile.absolutePath)
        add("--dest"); add(outputDir.get().asFile.absolutePath)
        add("--java-options"); add("-Xmx256m")
        add(if (isWindows) "--win-shortcut" else "--linux-shortcut")
        val iconFile = if (isWindows) rootProject.file("icons/ifsx.ico") else rootProject.file("icons/ifsx.png")
        if (iconFile.exists()) { add("--icon"); add(iconFile.absolutePath) }
        // Bundle manpage in the app image
        add("--app-content"); add(file("src/dist/man").absolutePath)
        // Post-install/pre-remove scripts for manpage + symlink
        if (isLinux) {
            add("--linux-deb-maintainer"); add("verginegiovanni@gmail.com")
            // postinst and prerm are picked up automatically by jpackage
            // when placed in the --resource-dir directory.
            if (jpackageRes.exists()) {
                add("--resource-dir"); add(jpackageRes.absolutePath)
            }
        }
    }

    doLast {
        val ext = if (isWindows) "msi" else "deb"
        val outDir = outputDir.get().asFile
        outDir.listFiles { f -> f.extension == ext }?.forEach { f ->
            // Insert -linux_ or -win- before the version number
            val newName = if (isWindows)
                f.name.replaceFirst(Regex("-(\\d)"), "-$platform-$1")
            else
                f.name.replaceFirst(Regex("_(\\d)"), "-${platform}_$1")
            if (newName != f.name) f.renameTo(File(outDir, newName))
        }
    }
}
