plugins {
    java
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

tasks.register("release") {
    description = "Build everything: installDist, distZip, jpackage for CLI and GUI"
    dependsOn(
        ":ifsx-cli:installDist",
        ":ifsx-cli:distZip",
        ":ifsx-cli:jpackage",
        ":ifsx-gui:installDist",
        ":ifsx-gui:distZip",
        ":ifsx-gui:jpackage"
    )
}