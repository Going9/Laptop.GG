plugins {
    id("org.springframework.boot")
}

tasks.register<JavaExec>("uiServer") {
    group = "verification"
    description = "Run the isolated UI fixture server (test classpath only)."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("going9.laptopgg.ui.UiTestServerKt")
}

dependencies {
    implementation(project(":application"))
    implementation(project(":recommendation-contract"))
    implementation(project(":infrastructure-jpa"))
    implementation(project(":infrastructure-security"))

    runtimeOnly(project(":infrastructure-flyway"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    runtimeOnly("org.postgresql:postgresql:42.7.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
