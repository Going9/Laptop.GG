plugins {
    `java-library`
}

dependencies {
    api(project(":persistence-model"))

    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
