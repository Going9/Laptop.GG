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

val verifyStructure by tasks.registering {
	group = "verification"
	description = "Verifies module boundary rules."

	doLast {
		fun textFiles(vararg paths: String) = paths.flatMap { path ->
			val file = project.file(path)
			when {
				!file.exists() -> emptyList()
				file.isFile -> listOf(file)
				else -> file.walkTopDown()
					.filter { it.isFile && it.extension in setOf("kt", "kts", "yml", "yaml") }
					.toList()
			}
		}

		fun assertAbsent(rule: String, paths: List<String>, patterns: List<Regex>) {
			val violations = textFiles(*paths.toTypedArray()).flatMap { file ->
				val relativePath = file.relativeTo(projectDir).path
				file.readLines().flatMapIndexed { index, line ->
					patterns
						.filter { pattern -> pattern.containsMatchIn(line) }
						.map { pattern -> "$relativePath:${index + 1}: ${pattern.pattern}" }
				}
			}

			check(violations.isEmpty()) {
				buildString {
					appendLine("Structure rule failed: $rule")
					violations.forEach { appendLine(it) }
				}
			}
		}

		assertAbsent(
			rule = "application must not depend on infrastructure or public web DTOs",
			paths = listOf("application/src/main", "application/src/test", "application/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.dto"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
				Regex("""spring-boot-starter-data-jpa"""),
				Regex("""com\.h2database"""),
			),
		)

		assertAbsent(
			rule = "application command and result contracts must not expose domain models",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/common",
				"application/src/main/kotlin/going9/laptopgg/application/comment/CommentModels.kt",
				"application/src/main/kotlin/going9/laptopgg/application/crawler/CrawlerPersistenceModels.kt",
				"application/src/main/kotlin/going9/laptopgg/application/crawler/SaveCrawledLaptopUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/crawler/TrackCrawlerRunUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/laptop/LaptopDetailResult.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/LaptopRecommendationQuery.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/LaptopRecommendationResult.kt",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.domain"""),
			),
		)

		assertAbsent(
			rule = "web-app must not use Spring Data pagination types",
			paths = listOf("web-app/src/main", "web-app/src/test", "web-app/build.gradle.kts"),
			patterns = listOf(
				Regex("""org\.springframework\.data\.domain"""),
				Regex("""Pageable"""),
				Regex("""PageRequest"""),
				Regex("""PageableDefault"""),
				Regex("""spring-data-commons"""),
			),
		)

		assertAbsent(
			rule = "web-app must not depend on domain models directly",
			paths = listOf("web-app/src/main", "web-app/src/test", "web-app/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.domain"""),
				Regex("""project\(":domain"\)"""),
			),
		)

		assertAbsent(
			rule = "web-app public packages must live under web",
			paths = listOf("web-app/src/main", "web-app/src/test"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.controller"""),
				Regex("""going9\.laptopgg\.dto"""),
				Regex("""package going9\.laptopgg\.controller"""),
				Regex("""package going9\.laptopgg\.dto"""),
			),
		)

		assertAbsent(
			rule = "web-app must not carry crawler runtime configuration",
			paths = listOf("web-app/src/main/resources/application.yml"),
			patterns = listOf(
				Regex("""app\.crawler"""),
				Regex("""CRAWLER_"""),
				Regex("""run-on-startup"""),
				Regex("""on-profile: crawler"""),
			),
		)

		assertAbsent(
			rule = "crawler-job must not carry web runtime configuration",
			paths = listOf("crawler-job/src/main/resources/application.yml"),
			patterns = listOf(
				Regex("""app\.api"""),
				Regex("""APP_BASE_URL"""),
				Regex("""management:"""),
				Regex("""server:"""),
				Regex("""on-profile: deploy"""),
			),
		)

		assertAbsent(
			rule = "crawler-job crawler implementation must live under job.crawler",
			paths = listOf("crawler-job/src/main", "crawler-job/src/test"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.service\.crawler"""),
				Regex("""package going9\.laptopgg\.service\.crawler"""),
			),
		)

		assertAbsent(
			rule = "crawler-job tests must not reach into JPA repositories directly",
			paths = listOf("crawler-job/src/main", "crawler-job/src/test", "crawler-job/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.infrastructure\.jpa"""),
				Regex("""spring-boot-starter-data-jpa"""),
			),
		)

		assertAbsent(
			rule = "crawler-job must depend on application contracts, not domain models or internal application services",
			paths = listOf("crawler-job/src/main", "crawler-job/src/test", "crawler-job/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.domain"""),
				Regex("""going9\.laptopgg\.application\.service"""),
				Regex("""project\(":domain"\)"""),
			),
		)
	}
}

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

	if (name == "domain") {
		apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
	}

	if (name in setOf("application", "infrastructure-jpa", "web-app", "crawler-job")) {
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
