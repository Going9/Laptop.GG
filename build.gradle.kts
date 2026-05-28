import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
	id("org.springframework.boot") version "3.5.14" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
	kotlin("jvm") version "1.9.25" apply false
	kotlin("plugin.spring") version "1.9.25" apply false
	kotlin("plugin.jpa") version "1.9.25" apply false
}

apply(from = "gradle/structure-check.gradle.kts")

allprojects {
	group = "Going9"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
	}
}

subprojects {
	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "io.spring.dependency-management")

	if (name in setOf("persistence-model", "persistence-model-web", "persistence-model-crawler")) {
		apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
	}

	if (name in setOf("infrastructure-jpa", "infrastructure-jpa-crawler", "infrastructure-security", "integration-tests", "web-app", "crawler-job")) {
		apply(plugin = "org.jetbrains.kotlin.plugin.spring")
	}

	extensions.configure<DependencyManagementExtension> {
		imports {
			mavenBom(SpringBootPlugin.BOM_COORDINATES)
		}
	}

	extensions.configure<JavaPluginExtension> {
		sourceCompatibility = JavaVersion.VERSION_17
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			freeCompilerArgs += "-Xjsr305=strict"
			jvmTarget = "17"
		}
	}

	tasks.withType<Test> {
		useJUnitPlatform()
		dependsOn(rootProject.tasks.named("verifyStructure"))
	}
}
