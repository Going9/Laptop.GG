plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":application-crawler"))
    implementation(project(":infrastructure-jpa"))
    implementation(project(":infrastructure-jpa-crawler"))

    implementation("org.jsoup:jsoup:1.18.3")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    compileOnly("org.springframework.data:spring-data-jpa")

    runtimeOnly("org.postgresql:postgresql:42.7.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
