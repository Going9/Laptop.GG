import org.gradle.api.artifacts.ProjectDependency

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

		fun projectDependencyPaths(projectPath: String, configurationName: String): Set<String> {
			val configuration = project(projectPath).configurations.findByName(configurationName) ?: return emptySet()
			return configuration.dependencies
				.withType(ProjectDependency::class.java)
				.map { dependency -> dependency.dependencyProject.path }
				.toSet()
		}

		fun assertProjectDependencies(
			rule: String,
			projectPath: String,
			configurationName: String,
			expectedProjectPaths: Set<String>,
		) {
			val actualProjectPaths = projectDependencyPaths(projectPath, configurationName)
			check(actualProjectPaths == expectedProjectPaths) {
				buildString {
					appendLine("Structure rule failed: $rule")
					appendLine("$projectPath $configurationName dependencies must be $expectedProjectPaths")
					appendLine("Actual: $actualProjectPaths")
				}
			}
		}

		assertAbsent(
			rule = "root build script must delegate structure verification rules",
			paths = listOf("build.gradle.kts"),
			patterns = listOf(
				Regex("""org\.gradle\.api\.artifacts\.ProjectDependency"""),
				Regex("""val\s+verifyStructure\s+by\s+tasks\.registering"""),
				Regex("""Structure rule failed:"""),
			),
		)

		mapOf(
			":laptop-taxonomy" to emptySet(),
			":persistence-model" to emptySet(),
			":recommendation-core" to emptySet(),
			":application" to setOf(":recommendation-core"),
			":application-crawler" to setOf(":laptop-taxonomy", ":recommendation-core"),
			":infrastructure-jpa-core" to emptySet(),
			":infrastructure-jpa" to setOf(":application", ":persistence-model", ":infrastructure-jpa-core"),
			":infrastructure-jpa-crawler" to setOf(":application-crawler", ":persistence-model", ":infrastructure-jpa-core"),
			":infrastructure-security" to setOf(":application"),
			":web-app" to setOf(":application", ":infrastructure-jpa", ":infrastructure-security", ":recommendation-core"),
			":crawler-job" to setOf(":application-crawler", ":infrastructure-jpa-core", ":infrastructure-jpa-crawler"),
			":integration-tests" to emptySet(),
		).forEach { (projectPath, expectedProjectPaths) ->
			assertProjectDependencies(
				rule = "runtime module dependency graph must stay top-down",
				projectPath = projectPath,
				configurationName = "implementation",
				expectedProjectPaths = expectedProjectPaths,
			)
		}

		mapOf(
			":laptop-taxonomy" to emptySet(),
			":persistence-model" to setOf(":laptop-taxonomy"),
			":recommendation-core" to emptySet(),
			":application" to emptySet(),
			":application-crawler" to emptySet(),
			":infrastructure-jpa-core" to emptySet(),
			":infrastructure-jpa" to emptySet(),
			":infrastructure-jpa-crawler" to emptySet(),
			":infrastructure-security" to emptySet(),
			":web-app" to emptySet(),
			":crawler-job" to emptySet(),
			":integration-tests" to emptySet(),
		).forEach { (projectPath, expectedProjectPaths) ->
			assertProjectDependencies(
				rule = "public module dependency graph must expose only shared taxonomy",
				projectPath = projectPath,
				configurationName = "api",
				expectedProjectPaths = expectedProjectPaths,
			)
		}

		assertProjectDependencies(
			rule = "integration tests may compose all persistence and application modules",
			projectPath = ":integration-tests",
			configurationName = "testImplementation",
			expectedProjectPaths = setOf(
				":application",
				":application-crawler",
				":laptop-taxonomy",
				":persistence-model",
				":infrastructure-jpa",
				":infrastructure-jpa-core",
				":infrastructure-jpa-crawler",
				":recommendation-core",
			),
		)

		assertPathAbsent(
			rule = "ops must be the only tracked operations config surface",
			paths = listOf("nginx"),
		)

		assertPathAbsent(
			rule = "JPA entity model must live in persistence-model, not legacy domain module",
			paths = listOf("domain"),
		)

		assertAbsent(
			rule = "legacy domain persistence namespace must not return",
			paths = listOf(
				"persistence-model/src/main",
				"infrastructure-jpa-core/src/main",
				"infrastructure-jpa/src/main",
				"infrastructure-jpa-crawler/src/main",
				"integration-tests/src/test",
				"settings.gradle.kts",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.domain"""),
				Regex("""project\(":domain"\)"""),
				Regex(""""domain""""),
			),
		)

		assertAbsent(
			rule = "laptop-taxonomy must stay a Spring-free shared enum module",
			paths = listOf("laptop-taxonomy/src/main", "laptop-taxonomy/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application"""),
				Regex("""going9\.laptopgg\.persistence"""),
				Regex("""going9\.laptopgg\.recommendation"""),
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.web"""),
				Regex("""org\.springframework"""),
				Regex("""spring-boot"""),
				Regex("""spring-context"""),
				Regex("""project\("""),
			),
		)

		assertAbsent(
			rule = "persistence-model must not depend on recommendation scoring policy",
			paths = listOf("persistence-model/src/main", "persistence-model/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.recommendation"""),
				Regex("""project\(":recommendation-core"\)"""),
				Regex("""RecommendationScoringPolicy"""),
			),
		)

		assertAbsent(
			rule = "application must not depend on persistence models, infrastructure, or public web DTOs",
			paths = listOf("application/src/main", "application/src/test", "application/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.application\.crawler"""),
				Regex("""going9\.laptopgg\.dto"""),
				Regex("""project\(":persistence-model"\)"""),
				Regex("""project\(":application-crawler"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
				Regex("""spring-boot-starter-data-jpa"""),
				Regex("""spring-security-crypto"""),
				Regex("""org\.springframework\.security"""),
				Regex("""org\.springframework\.transaction"""),
				Regex("""@Transactional"""),
				Regex("""spring-tx"""),
				Regex("""com\.h2database"""),
			),
		)

		assertAbsent(
			rule = "application must not define crawler persistence contracts",
			paths = listOf("application/src/main", "application/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model\.crawler"""),
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
			rule = "web-facing application ports must not expose persistence models",
			paths = listOf("application/src/main/kotlin/going9/laptopgg/application"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model"""),
			),
		)

		assertPathAbsent(
			rule = "web-facing application ports must live under feature packages, not a generic port.out package",
			paths = listOf("application/src/main/kotlin/going9/laptopgg/application/port/out"),
		)

		assertAbsent(
			rule = "application-crawler must not depend on infrastructure, persistence, or web",
			paths = listOf("application-crawler/src/main", "application-crawler/src/test", "application-crawler/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""going9\.laptopgg\.web"""),
				Regex("""going9\.laptopgg\.application\.recommendation"""),
				Regex("""going9\.laptopgg\.application\.service"""),
				Regex("""going9\.laptopgg\.application\.port\.out"""),
				Regex("""going9\.laptopgg\.application\.(comment|common|laptop|recommendation)\.port"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":persistence-model"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
				Regex("""project\(":web-app"\)"""),
				Regex("""spring-boot-starter-data-jpa"""),
			),
		)

		assertAbsent(
			rule = "application-crawler code must live in feature or port packages, not the flat crawler package",
			paths = listOf("application-crawler/src/main", "application-crawler/src/test"),
			patterns = listOf(
				Regex("""^package going9\.laptopgg\.application\.crawler$"""),
			),
		)

		assertAbsent(
			rule = "application-crawler must keep Spring transaction infrastructure behind ports",
			paths = listOf("application-crawler/src/main", "application-crawler/src/test", "application-crawler/build.gradle.kts"),
			patterns = listOf(
				Regex("""org\.springframework\.transaction"""),
				Regex("""@Transactional"""),
				Regex("""spring-tx"""),
				Regex("""kotlin\.plugin\.spring"""),
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
			rule = "crawler price history port must not expose JPA entities",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/price/port/LaptopPriceHistoryPort.kt"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""fun\s+save\(.*LaptopPriceHistory"""),
			),
		)

		assertAbsent(
			rule = "crawler recommendation score port must not expose JPA entities",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/port/RecommendationScorePort.kt"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""List<RecommendationScore>"""),
				Regex("""Iterable<RecommendationScore>"""),
			),
		)

		assertAbsent(
			rule = "crawler profile port must not expose JPA entities",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/port/CrawledLaptopProfilePort.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/port/CrawledLaptopProfileSourcePort.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/LaptopProfileService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreService.kt",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model\.laptop\.LaptopProfile"""),
				Regex("""LaptopProfile\("""),
			),
		)

		assertAbsent(
			rule = "crawler laptop persistence boundary must not expose JPA entities",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/port/CrawledLaptopPersistencePort.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/port/CrawledLaptopProfileSourcePort.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/price/LaptopPriceHistoryService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/LaptopProfileService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/LaptopProfileFactory.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/CpuClassifier.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/GpuClassifier.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/ProfileScorePolicy.kt",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model\.laptop\.Laptop\b"""),
				Regex("""going9\.laptopgg\.persistence\.model\.laptop\.LaptopUsage\b"""),
			),
		)

		assertPathAbsent(
			rule = "crawler laptop ports must be split by use case responsibility",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/port/out/CrawledLaptopPort.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/port/out",
			),
		)

		assertAbsent(
			rule = "crawler save use case must delegate laptop change detection",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopService.kt"),
			patterns = listOf(
				Regex("""UpdateCrawledLaptopCommand\("""),
				Regex("""fun\s+changed(Text|Present|Usages)\b"""),
				Regex("""fun\s+PersistedCrawledLaptopSnapshot\.toUpdateCommand\b"""),
				Regex("""fun\s+UpdateCrawledLaptopCommand\.hasChanges\b"""),
			),
		)

		assertAbsent(
			rule = "crawler run port must not expose JPA entities",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/port/CrawlerRunPort.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunService.kt",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model\.crawler"""),
				Regex("""CrawlerRun\("""),
				Regex("""CrawlerRunStatus\."""),
			),
		)

		assertAbsent(
			rule = "infrastructure-jpa-core must not define runtime repositories",
			paths = listOf("infrastructure-jpa-core/src/main"),
			patterns = listOf(
				Regex("""JpaRepository"""),
				Regex("""EnableJpaRepositories"""),
				Regex("""repository\.shared"""),
				Regex("""findByName"""),
				Regex("""findAllWithoutProfile"""),
				Regex("""findAllIncompleteStaticScores"""),
				Regex("""findAllWithLaptopAndUsage"""),
			),
		)

		assertAbsent(
			rule = "infrastructure-jpa-core must not own runtime entity scanning",
			paths = listOf("infrastructure-jpa-core/src/main", "infrastructure-jpa-core/build.gradle.kts"),
			patterns = listOf(
				Regex("""EntityScan"""),
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""project\(":persistence-model"\)"""),
			),
		)

		assertAbsent(
			rule = "recommendation-core must stay a Spring-free scoring policy module",
			paths = listOf("recommendation-core/src/main", "recommendation-core/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application"""),
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""going9\.laptopgg\.taxonomy"""),
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.web"""),
				Regex("""org\.springframework"""),
				Regex("""spring-boot"""),
				Regex("""spring-context"""),
				Regex("""project\(":laptop-taxonomy"\)"""),
				Regex("""project\(":persistence-model"\)"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":application-crawler"\)"""),
			),
		)

		assertAbsent(
			rule = "infrastructure-jpa web adapter module must stay free of crawler persistence adapters",
			paths = listOf("infrastructure-jpa/src/main", "infrastructure-jpa/src/test", "infrastructure-jpa/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application\.crawler"""),
				Regex("""infrastructure\.jpa\.adapter\.crawler"""),
				Regex("""infrastructure\.jpa\.repository\.crawler"""),
				Regex("""CrawlerRunRepository"""),
				Regex("""LaptopPriceHistoryRepository"""),
				Regex("""RecommendationScoreRepository"""),
				Regex("""(?m)^\s*implementation\(project\(":application-crawler"\)\)"""),
				Regex("""(?m)^\s*testImplementation\(project\(":application-crawler"\)\)"""),
				Regex("""(?m)^\s*implementation\(project\(":infrastructure-jpa-crawler"\)\)"""),
				Regex("""(?m)^\s*testImplementation\(project\(":infrastructure-jpa-crawler"\)\)"""),
			),
		)

		assertAbsent(
			rule = "web JPA config must not scan crawler-only persistence entities",
			paths = listOf("infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/config/WebJpaRepositoryConfig.kt"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model\.crawler"""),
			),
		)

		assertAbsent(
			rule = "web JPA repositories must not expose crawler lookup or backfill methods",
			paths = listOf("infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/repository/web"),
			patterns = listOf(
				Regex("""findByProductCode"""),
				Regex("""findAllByProductCode"""),
				Regex("""findAllByDetailPage"""),
				Regex("""findIdsWithoutProfile"""),
				Regex("""findLaptopIdsWithIncompleteStaticScores"""),
			),
		)

		assertPathAbsent(
			rule = "web JPA adapters must be named after their application port responsibility",
			paths = listOf(
				"infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web/LaptopJpaAdapter.kt",
				"infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web/LaptopProfileJpaAdapter.kt",
			),
		)

		assertAbsent(
			rule = "web JPA adapters must implement a single application port responsibility",
			paths = listOf("infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web"),
			patterns = listOf(
				Regex("""\)\s*:\s*.*Port,\s*.*Port"""),
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
				Regex("""going9\.laptopgg\.application\.(comment|common|laptop|recommendation)\.port"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
			),
		)

		assertAbsent(
			rule = "crawler JPA config must not scan web-only persistence entities",
			paths = listOf("infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/config/CrawlerJpaRepositoryConfig.kt"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model\.web"""),
			),
		)

		assertAbsent(
			rule = "crawler JPA repositories must not expose web recommendation query methods",
			paths = listOf("infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/repository/crawler"),
			patterns = listOf(
				Regex("""findRecommendationCandidates"""),
				Regex("""findRecommendationCandidatePage"""),
			),
		)

		assertPathAbsent(
			rule = "crawler JPA adapters must be named after their application port responsibility",
			paths = listOf("infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopJpaAdapter.kt"),
		)

		assertAbsent(
			rule = "crawler JPA adapters must implement a single application port responsibility",
			paths = listOf("infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler"),
			patterns = listOf(
				Regex("""\)\s*:\s*.*Port,\s*.*Port"""),
			),
		)

		assertAbsent(
			rule = "application command and result contracts must not expose persistence models",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/common",
				"application/src/main/kotlin/going9/laptopgg/application/comment/CommentModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/CrawlerPersistenceModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopUseCase.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/price/LaptopPriceHistoryModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunLockUseCase.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/laptop/LaptopDetailResult.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/LaptopRecommendationQuery.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/LaptopRecommendationResult.kt",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model"""),
			),
		)

		assertAbsent(
			rule = "application pagination contracts must not expose raw sort strings",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/common/PageQuery.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/port/RecommendationCandidatePort.kt",
			),
			patterns = listOf(
				Regex("""val property: String"""),
				Regex("""val sortMode: String"""),
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
			rule = "web-app must not depend on persistence models directly",
			paths = listOf("web-app/src/main", "web-app/src/test", "web-app/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""going9\.laptopgg\.application\.crawler"""),
				Regex("""project\(":persistence-model"\)"""),
				Regex("""project\(":application-crawler"\)"""),
				Regex("""project\(":infrastructure-jpa-crawler"\)"""),
			),
		)

		assertAbsent(
			rule = "page controllers must not own recommendation view option catalogs",
			paths = listOf("web-app/src/main/kotlin/going9/laptopgg/web/controller"),
			patterns = listOf(
				Regex("""UseCaseOption"""),
				Regex("""ScreenSizeModeOption"""),
				Regex("""fun\s+useCaseLabel"""),
				Regex("""fun\s+useCaseHeading"""),
				Regex("""fun\s+screenSizeSummary"""),
				Regex("""fun\s+budgetPresetList"""),
				Regex("""fun\s+weightPresetList"""),
			),
		)

		assertPathAbsent(
			rule = "web page routes must stay split by feature instead of a generic PageController",
			paths = listOf(
				"web-app/src/main/kotlin/going9/laptopgg/web/controller/PageController.kt",
				"web-app/src/test/kotlin/going9/laptopgg/web/controller/PageControllerTest.kt",
			),
		)

		assertAbsent(
			rule = "infrastructure-security must stay free of JPA persistence concerns",
			paths = listOf("infrastructure-security/src/main", "infrastructure-security/src/test", "infrastructure-security/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""going9\.laptopgg\.infrastructure\.jpa"""),
				Regex("""spring-boot-starter-data-jpa"""),
				Regex("""JpaRepository"""),
			),
		)

		assertAbsent(
			rule = "infrastructure-security must not hardcode bcrypt strength",
			paths = listOf("infrastructure-security/src/main"),
			patterns = listOf(
				Regex("""BCryptPasswordEncoder\(\s*\d+"""),
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
			rule = "thymeleaf templates must keep CSS and behavior in static resources",
			paths = listOf("web-app/src/main/resources/templates"),
			patterns = listOf(
				Regex("""<style\b[^>]*>""", RegexOption.IGNORE_CASE),
				Regex("""<script(?![^>]*(?:\bsrc=|\bth:src=))[^>]*>""", RegexOption.IGNORE_CASE),
				Regex("""\sstyle\s*=""", RegexOption.IGNORE_CASE),
				Regex("""(?:\s|:)on[a-z]+\s*=""", RegexOption.IGNORE_CASE),
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
			rule = "crawler runner must use typed app.crawler configuration properties",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/runner"),
			patterns = listOf(
				Regex("""org\.springframework\.beans\.factory\.annotation\.Value"""),
				Regex("""getOptionValues\("app\.crawler"""),
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
			rule = "Danawa endpoint client must not own low-level HTTP retry or pacing",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/client/DanawaClient.kt"),
			patterns = listOf(
				Regex("""java\.net\.http\.HttpClient"""),
				Regex("""java\.net\.http\.HttpResponse"""),
				Regex("""java\.io\.IOException"""),
				Regex("""ThreadLocalRandom"""),
				Regex("""requestPacingLock"""),
				Regex("""MAX_HTTP_RETRIES"""),
				Regex("""RETRYABLE_STATUS_CODES"""),
				Regex("""fun\s+(awaitRequestSlot|extendGlobalCooldown|retryDelayMillis|randomJitterMillis)\b"""),
			),
		)

		assertAbsent(
			rule = "CrawlerService must delegate source page traversal",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""ListPageCrawler"""),
				Regex("""CrawlProductBatchProcessor"""),
				Regex("""DanawaListParser"""),
				Regex("""ProductCard"""),
				Regex("""DuplicateTailStopPolicy"""),
				Regex("""fetchProductPageBatch"""),
				Regex("""while\s*\(currentPage"""),
				Regex("""currentPage\+\+"""),
			),
		)

		assertAbsent(
			rule = "runtime applications must not own JPA repository scanning",
			paths = listOf(
				"web-app/src/main",
				"web-app/build.gradle.kts",
				"crawler-job/src/main",
				"crawler-job/build.gradle.kts",
			),
			patterns = listOf(
				Regex("""EnableJpaRepositories"""),
				Regex("""org\.springframework\.data\.jpa\.repository\.config"""),
				Regex("""compileOnly\("org\.springframework\.data:spring-data-jpa"\)"""),
			),
		)

		assertAbsent(
			rule = "integration tests must reuse infrastructure JPA repository configuration",
			paths = listOf("integration-tests/src/test"),
			patterns = listOf(
				Regex("""EnableJpaRepositories"""),
				Regex("""org\.springframework\.data\.jpa\.repository\.config"""),
			),
		)

		assertPathAbsent(
			rule = "infrastructure-backed integration tests must not live under application package paths",
			paths = listOf("integration-tests/src/test/kotlin/going9/laptopgg/application"),
		)

		assertPathAbsent(
			rule = "recommendation integration tests must stay split by test responsibility",
			paths = listOf("integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/RecommendLaptopsUseCaseIntegrationTest.kt"),
		)

		assertAbsent(
			rule = "infrastructure-backed integration tests must use integration packages",
			paths = listOf("integration-tests/src/test"),
			patterns = listOf(
				Regex("""^package going9\.laptopgg\.application\."""),
			),
		)

		assertAbsent(
			rule = "recommendation integration scenario tests must keep persistence fixtures in support classes",
			paths = listOf(
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/RecommendationCandidateFilteringIntegrationTest.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/RecommendationOrderingIntegrationTest.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/RecommendationProjectionIntegrationTest.kt",
			),
			patterns = listOf(
				Regex("""fun\s+(persistLaptop|persistSortProbeLaptops|overrideProfileScores|saveProfileAndScores)\b"""),
				Regex("""fun\s+Laptop\.toCrawledSnapshot"""),
				Regex("""LaptopUsage\("""),
				Regex("""PersistedCrawledLaptopSnapshot"""),
			),
		)

		assertAbsent(
			rule = "recommendation integration scenario tests must reuse shared Spring support",
			paths = listOf(
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/RecommendationCandidateFilteringIntegrationTest.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/RecommendationOrderingIntegrationTest.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/RecommendationProjectionIntegrationTest.kt",
			),
			patterns = listOf(
				Regex("""@SpringBootTest"""),
				Regex("""@Transactional"""),
				Regex("""@Autowired"""),
				Regex("""CrawlerLaptop(Profile)?Repository"""),
				Regex("""RecommendationScoreRepository"""),
				Regex("""RecommendationIntegrationFixtures\("""),
			),
		)

		assertAbsent(
			rule = "integration test application must compose use case configs instead of owning beans",
			paths = listOf("integration-tests/src/test/kotlin/going9/laptopgg/InfrastructureJpaTestApplication.kt"),
			patterns = listOf(
				Regex("""@Bean"""),
				Regex("""fun\s+\w+\("""),
				Regex("""application\.crawler\.(common|persistence|price|profile|recommendation|run)\.port"""),
				Regex("""application\.recommendation\.port"""),
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
			rule = "crawler-job must depend on application-crawler contracts, not persistence models or web application contracts",
			paths = listOf("crawler-job/src/main", "crawler-job/src/test", "crawler-job/build.gradle.kts"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""going9\.laptopgg\.application\.port\.out"""),
				Regex("""going9\.laptopgg\.application\.(comment|common|laptop|recommendation)\.port"""),
				Regex("""going9\.laptopgg\.application\.recommendation"""),
				Regex("""going9\.laptopgg\.application\.service"""),
				Regex("""project\(":persistence-model"\)"""),
				Regex("""project\(":application"\)"""),
				Regex("""project\(":infrastructure-jpa"\)"""),
			),
		)

		assertAbsent(
			rule = "crawler job runners and crawler implementation must not call crawler out ports directly",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler",
				"crawler-job/src/test",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.application\.crawler\.port\.out"""),
			),
		)

		assertAbsent(
			rule = "crawler-job crawler code must live in role packages, not the flat crawler package",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler", "crawler-job/src/test/kotlin/going9/laptopgg/job/crawler"),
			patterns = listOf(
				Regex("""^package going9\.laptopgg\.job\.crawler$"""),
			),
		)

		assertAbsent(
			rule = "crawler orchestration must not own filter profile source mapping",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""fun\s+resolveFilterProfile"""),
				Regex("""fun\s+resolveCrawlSources"""),
				Regex("""CrawlerFilterSets"""),
				Regex("""APPLE_MACBOOK_LIST_URL"""),
			),
		)

		assertAbsent(
			rule = "crawler orchestration must not own progress counters and samples",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""var\s+(processedCount|createdCount|updatedCount|degradedCount|priceOnlyUpdatedCount|detailRefreshCount|failedCount)"""),
				Regex("""degradedSamples"""),
				Regex("""failureSamples"""),
				Regex("""fun\s+recordSample"""),
			),
		)

		assertAbsent(
			rule = "crawler orchestration must not expose parser or merger test delegates",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""LaptopSnapshotMerger"""),
				Regex("""resolveCpuModel"""),
			),
		)

		assertAbsent(
			rule = "crawler orchestration must not own product batch persistence flow",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""SaveCrawledLaptopUseCase"""),
				Regex("""DetailCrawler"""),
				Regex("""DetailRefreshWorkItem"""),
				Regex("""DetailRefreshOutcome"""),
				Regex("""loadExistingLookup"""),
				Regex("""saveListSnapshot"""),
				Regex("""saveOrUpdateLaptop"""),
			),
		)

		assertAbsent(
			rule = "crawler orchestration must not expose duplicate-tail stop policy",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""shouldStopAtDuplicateTail"""),
				Regex("""MAX_CONSECUTIVE_DUPLICATE_ONLY_PAGES"""),
			),
		)

		assertAbsent(
			rule = "crawler orchestration must get operational tuning from typed configuration",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""MAX_LIST_PAGES"""),
				Regex("""DETAIL_FETCH_CONCURRENCY"""),
			),
		)

		assertAbsent(
			rule = "crawler-job must not implement PostgreSQL lock infrastructure directly",
			paths = listOf("crawler-job/src/main", "crawler-job/src/test"),
			patterns = listOf(
				Regex("""javax\.sql\.DataSource"""),
				Regex("""pg_try_advisory_lock"""),
				Regex("""pg_advisory_unlock"""),
				Regex("""CrawlerAdvisoryLockService"""),
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

		assertAbsent(
			rule = "crawler-job must not keep GPU classification keyword policy outside application-crawler classifiers",
			paths = listOf("crawler-job/src/main"),
			patterns = listOf(
				Regex("""DISCRETE_GPU_KEYWORDS"""),
				Regex("""INTEGRATED_GPU_KEYWORDS"""),
				Regex("""isIntegratedGraphics"""),
			),
		)
	}
}
