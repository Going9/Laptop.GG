plugins {
    `java-library`
}

dependencies {
    api(project(":laptop-taxonomy"))
    api(project(":recommendation-contract"))
    implementation(project(":recommendation-core"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
