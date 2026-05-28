dependencies {
    implementation(project(":laptop-taxonomy"))
    implementation(project(":recommendation-contract"))
    implementation(project(":recommendation-core"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
