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

		fun assertPathAbsent(rule: String, paths: List<String>) {
			val violations = paths
				.map(project::file)
				.filter { it.exists() }
				.map { it.relativeTo(projectDir).path }

			check(violations.isEmpty()) {
				buildString {
					appendLine("Structure rule failed: $rule")
					violations.forEach { appendLine(it) }
				}
			}
		}

		assertPathAbsent(
			rule = "ops must be the only tracked operations config surface",
			paths = listOf("nginx"),
		)

		assertAbsent(
			rule = "application must not depend on infrastructure or public web DTOs",
			paths = listOf("application/src/main", "application/src/test", "application/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.application\.crawler"""),
				Regex("""going9\.laptopgg\.dto"""),
				Regex("""project\(":application-crawler"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
				Regex("""spring-boot-starter-data-jpa"""),
				Regex("""spring-security-crypto"""),
				Regex("""org\.springframework\.security"""),
				Regex("""com\.h2database"""),
			),
		)

		assertAbsent(
			rule = "application must not define crawler persistence contracts",
			paths = listOf("application/src/main", "application/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.domain\.crawler"""),
				Regex("""CrawlerRunPort"""),
				Regex("""LaptopPriceHistoryPort"""),
				Regex("""RecommendationScorePort"""),
				Regex("""findIdsWithoutProfile"""),
				Regex("""findLaptopIdsWithIncompleteStaticScores"""),
				Regex("""findAllByProductCodes"""),
				Regex("""findAllByDetailPages"""),
				Regex("""findAllByDetailPageContaining"""),
				Regex("""findByProductCode"""),
			),
		)

		assertAbsent(
			rule = "application-crawler must not depend on infrastructure or web",
			paths = listOf("application-crawler/src/main", "application-crawler/src/test", "application-crawler/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.web"""),
				Regex("""going9\.laptopgg\.application\.recommendation"""),
				Regex("""going9\.laptopgg\.application\.service"""),
				Regex("""going9\.laptopgg\.application\.port\.out"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
				Regex("""project\(":web-app"\)"""),
				Regex("""spring-boot-starter-data-jpa"""),
			),
		)

		assertAbsent(
			rule = "crawler persistence must not use detail_page substring fallback",
			paths = listOf("application-crawler/src/main", "infrastructure-jpa-core/src/main", "infrastructure-jpa-crawler/src/main"),
			patterns = listOf(
				Regex("""findAllByDetailPageContaining"""),
				Regex("""allowLegacyFallback"""),
				Regex("""detailPageToken"""),
			),
		)

		assertAbsent(
			rule = "recommendation-core must stay a Spring-free shared policy module",
			paths = listOf("recommendation-core/src/main", "recommendation-core/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application"""),
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.web"""),
				Regex("""org\.springframework"""),
				Regex("""spring-boot"""),
				Regex("""spring-context"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":application-crawler"\)"""),
			),
		)

		assertAbsent(
			rule = "infrastructure-jpa web adapter module must stay free of crawler persistence adapters",
			paths = listOf("infrastructure-jpa/src/main", "infrastructure-jpa/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application\.crawler"""),
				Regex("""infrastructure\.jpa\.adapter\.crawler"""),
				Regex("""infrastructure\.jpa\.repository\.crawler"""),
				Regex("""CrawlerRunRepository"""),
				Regex("""LaptopPriceHistoryRepository"""),
				Regex("""RecommendationScoreRepository"""),
				Regex("""(?m)^\s*implementation\(project\(":application-crawler"\)\)"""),
				Regex("""(?m)^\s*implementation\(project\(":infrastructure-jpa-crawler"\)\)"""),
			),
		)

		assertAbsent(
			rule = "infrastructure-jpa-core must stay free of application ports and runtime adapters",
			paths = listOf("infrastructure-jpa-core/src/main", "infrastructure-jpa-core/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application"""),
				Regex("""going9\.laptopgg\.web"""),
				Regex("""infrastructure\.jpa\.adapter"""),
				Regex("""org\.springframework\.stereotype"""),
				Regex("""@Component"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":application-crawler"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
				Regex("""project\(":infrastructure-jpa-crawler"\)"""),
			),
		)

		assertAbsent(
			rule = "infrastructure-jpa-crawler must implement crawler ports without direct web application contracts",
			paths = listOf("infrastructure-jpa-crawler/src/main", "infrastructure-jpa-crawler/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application\.port\.out"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
			),
		)

		assertAbsent(
			rule = "application command and result contracts must not expose domain models",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/common",
				"application/src/main/kotlin/going9/laptopgg/application/comment/CommentModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/CrawlerPersistenceModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/SaveCrawledLaptopUseCase.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/TrackCrawlerRunUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/laptop/LaptopDetailResult.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/LaptopRecommendationQuery.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/LaptopRecommendationResult.kt",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.domain"""),
			),
		)

		assertAbsent(
			rule = "application recommendation contracts must not carry legacy public request aliases",
			paths = listOf("application/src/main/kotlin/going9/laptopgg/application/recommendation"),
			patterns = listOf(
				Regex("""LegacyRecommendationPurpose"""),
				Regex("""\bpurpose\b"""),
			),
		)

		assertAbsent(
			rule = "application user-flow code must live in feature packages, not a generic service package",
			paths = listOf("application/src/main"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application\.service"""),
				Regex("""package going9\.laptopgg\.application\.service"""),
			),
		)

		assertAbsent(
			rule = "application services must not encode runtime Spring profiles",
			paths = listOf("application/src/main", "application-crawler/src/main"),
			patterns = listOf(
				Regex("""org\.springframework\.context\.annotation\.Profile"""),
				Regex("""@Profile"""),
			),
		)

		assertAbsent(
			rule = "application modules must not use Spring component-scan stereotypes",
			paths = listOf("application/src/main", "application-crawler/src/main", "application/build.gradle.kts", "application-crawler/build.gradle.kts"),
			patterns = listOf(
				Regex("""org\.springframework\.stereotype"""),
				Regex("""@Service"""),
				Regex("""@Component"""),
				Regex("""spring-context"""),
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
				Regex("""going9\.laptopgg\.application\.crawler"""),
				Regex("""project\(":domain"\)"""),
				Regex("""project\(":application-crawler"\)"""),
				Regex("""project\(":infrastructure-jpa-crawler"\)"""),
			),
		)

		assertAbsent(
			rule = "infrastructure-security must stay free of JPA and domain persistence concerns",
			paths = listOf("infrastructure-security/src/main", "infrastructure-security/src/test", "infrastructure-security/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.domain"""),
				Regex("""going9\.laptopgg\.infrastructure\.jpa"""),
				Regex("""spring-boot-starter-data-jpa"""),
				Regex("""JpaRepository"""),
			),
		)

		assertAbsent(
			rule = "runtime modules must import shared persistence config instead of owning datasource or JPA settings",
			paths = listOf("web-app/src/main/resources/application.yml", "crawler-job/src/main/resources/application.yml"),
			patterns = listOf(
				Regex("""(?m)^\s{2}datasource:"""),
				Regex("""(?m)^\s{2}jpa:"""),
				Regex("""ddl-auto:"""),
				Regex("""baseline-version:"""),
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
			rule = "web-app must not keep legacy form templates or routes",
			paths = listOf("web-app/src/main"),
			patterns = listOf(
				Regex("""spec-form"""),
				Regex("""foreignLaptop"""),
				Regex("""gaming-options"""),
				Regex("""tenkey"""),
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
			rule = "crawler-job runtime support must live under job packages",
			paths = listOf("crawler-job/src/main", "crawler-job/src/test"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.config"""),
				Regex("""going9\.laptopgg\.runner"""),
				Regex("""package going9\.laptopgg\.config"""),
				Regex("""package going9\.laptopgg\.runner"""),
			),
		)

		assertAbsent(
			rule = "crawler-job must not use root package default component scan",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/CrawlerJobApplication.kt"),
			patterns = listOf(
				Regex("""@SpringBootApplication\s*$"""),
			),
		)

		assertAbsent(
			rule = "web-app must not use root package default component scan",
			paths = listOf("web-app/src/main/kotlin/going9/laptopgg/LaptopGgApplication.kt"),
			patterns = listOf(
				Regex("""@SpringBootApplication\s*$"""),
				Regex(""""going9\.laptopgg\.infrastructure\.jpa","""),
				Regex(""""going9\.laptopgg\.infrastructure\.jpa\.repository\.crawler","""),
			),
		)

		assertAbsent(
			rule = "crawler-job must not scan all JPA adapters",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/CrawlerJobApplication.kt"),
			patterns = listOf(
				Regex(""""going9\.laptopgg\.infrastructure\.jpa","""),
				Regex(""""going9\.laptopgg\.infrastructure\.jpa\.adapter\.shared","""),
				Regex(""""going9\.laptopgg\.config","""),
				Regex(""""going9\.laptopgg\.runner","""),
			),
		)

		assertAbsent(
			rule = "crawler-job must not register web repositories",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/CrawlerJobApplication.kt"),
			patterns = listOf(
				Regex(""""going9\.laptopgg\.infrastructure\.jpa\.repository\.web","""),
			),
		)

		assertAbsent(
			rule = "crawler-job tests must not reach into JPA repositories directly",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job",
				"crawler-job/src/main/kotlin/going9/laptopgg/runner",
				"crawler-job/src/test",
				"crawler-job/build.gradle.kts",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.infrastructure\.jpa"""),
				Regex("""spring-boot-starter-data-jpa"""),
			),
		)

		assertAbsent(
			rule = "crawler-job must depend on application-crawler contracts, not domain models or web application contracts",
			paths = listOf("crawler-job/src/main", "crawler-job/src/test", "crawler-job/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.domain"""),
				Regex("""going9\.laptopgg\.application\.port\.out"""),
				Regex("""going9\.laptopgg\.application\.recommendation"""),
				Regex("""going9\.laptopgg\.application\.service"""),
				Regex("""project\(":domain"\)"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
			),
		)

		assertAbsent(
			rule = "crawler-job must not keep legacy CPU manufacturer maps outside application-crawler classifiers",
			paths = listOf("crawler-job/src/main", "crawler-job/src/test"),
			patterns = listOf(
				Regex("""CpuModelMap"""),
				Regex("""cpuModelMap"""),
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

	if (name in setOf("application", "application-crawler", "infrastructure-jpa", "infrastructure-jpa-crawler", "infrastructure-security", "web-app", "crawler-job")) {
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
