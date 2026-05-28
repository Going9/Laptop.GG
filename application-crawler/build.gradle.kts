dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))

    implementation("org.springframework:spring-tx")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
