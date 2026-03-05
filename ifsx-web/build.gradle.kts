plugins {
    war
}

dependencies {
    implementation(project(":ifsx-core"))
    providedCompile("jakarta.servlet:jakarta.servlet-api:${property("servletVersion")}")
}
