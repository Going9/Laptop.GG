dependencies {
    implementation(project(":application"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.security:spring-security-crypto")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
