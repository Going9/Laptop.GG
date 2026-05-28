plugins {
    `java-library`
}

dependencies {
    api(project(":recommendation-core"))

    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
}
