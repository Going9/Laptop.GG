dependencies {
    implementation(project(":infrastructure-jpa-core"))
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    runtimeOnly("org.postgresql:postgresql:42.7.5")

    testImplementation(project(":application-crawler"))
    testImplementation(project(":infrastructure-jpa-crawler"))
    testImplementation(project(":recommendation-core"))
    testImplementation("org.flywaydb:flyway-core")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
