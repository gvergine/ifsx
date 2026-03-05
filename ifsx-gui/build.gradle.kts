plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    version = property("javafxVersion") as String
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("io.github.gvergine.ifsx.gui.Launcher")
}

dependencies {
    implementation(project(":ifsx-core"))
}

val platform = if (System.getProperty("os.name").lowercase().contains("win")) "win" else "linux"

tasks.named<Zip>("distZip") {
    archiveBaseName.set("${project.name}-$platform")
}

// Native installer via jpackage
tasks.register<Exec>("jpackage") {
    dependsOn("installDist")
    val installDir = layout.buildDirectory.dir("install/ifsx-gui")
    val outputDir = layout.buildDirectory.dir("jpackage")
    val appVersion = project.version.toString()

    doFirst {
        outputDir.get().asFile.mkdirs()
    }

    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val isLinux = System.getProperty("os.name").lowercase().contains("linux")

    val jpackageRes = file("src/jpackage")

    commandLine = buildList {
        add("jpackage")
        add("--type"); add(if (isWindows) "msi" else if (isLinux) "deb" else "dmg")
        add("--name"); add("ifsx-gui")
        add("--app-version"); add(appVersion)
        add("--vendor"); add("Giovanni Vergine")
        add("--description"); add("IFSX -- IFS Extract/Repack Tool")
        add("--input"); add(installDir.get().dir("lib").asFile.absolutePath)
        add("--main-jar"); add("ifsx-gui-${appVersion}.jar")
        add("--main-class"); add("io.github.gvergine.ifsx.gui.Launcher")
        add("--dest"); add(outputDir.get().asFile.absolutePath)
        add("--java-options"); add("-Xmx512m")
        val iconFile = if (isWindows) rootProject.file("icons/ifsx.ico") else rootProject.file("icons/ifsx.png")
        if (iconFile.exists()) { add("--icon"); add(iconFile.absolutePath) }
        if (isLinux || isWindows) { add(if (isWindows) "--win-shortcut" else "--linux-shortcut") }
        if (isLinux) {
            add("--linux-deb-maintainer"); add("verginegiovanni@gmail.com")
            if (jpackageRes.exists()) {
                add("--resource-dir"); add(jpackageRes.absolutePath)
            }
        }
    }

    doLast {
        val ext = if (isWindows) "msi" else "deb"
        val outDir = outputDir.get().asFile
        outDir.listFiles { f -> f.extension == ext }?.forEach { f ->
            val newName = if (isWindows)
                f.name.replaceFirst(Regex("-(\\d)"), "-$platform-$1")
            else
                f.name.replaceFirst(Regex("_(\\d)"), "-${platform}_$1")
            if (newName != f.name) f.renameTo(File(outDir, newName))
        }
    }
}
