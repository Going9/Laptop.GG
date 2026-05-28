dependencies {
    testImplementation(project(":application"))
    testImplementation(project(":application-crawler"))
    testImplementation(project(":persistence-model"))
    testImplementation(project(":infrastructure-jpa"))
    testImplementation(project(":infrastructure-jpa-core"))
    testImplementation(project(":infrastructure-jpa-crawler"))
    testImplementation(project(":recommendation-core"))

    testImplementation("org.flywaydb:flyway-core")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
