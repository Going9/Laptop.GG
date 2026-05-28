plugins {
    `java-library`
}

dependencies {
    api(project(":laptop-taxonomy"))

    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
