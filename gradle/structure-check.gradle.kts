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

		fun assertPresent(rule: String, paths: List<String>, patterns: List<Regex>) {
			val text = textFiles(*paths.toTypedArray())
				.joinToString(separator = "\n") { file -> file.readText() }
			val missingPatterns = patterns
				.filterNot { pattern -> pattern.containsMatchIn(text) }
				.map { pattern -> pattern.pattern }

			check(missingPatterns.isEmpty()) {
				buildString {
					appendLine("Structure rule failed: $rule")
					missingPatterns.forEach { appendLine("missing: $it") }
				}
			}
		}

		fun assertOrdered(rule: String, path: String, patterns: List<Regex>) {
			val file = project.file(path)
			val text = file.readText()
			var searchStart = 0
			val missingPatterns = mutableListOf<String>()

			for (pattern in patterns) {
				val match = pattern.find(text, searchStart)
				if (match == null) {
					missingPatterns += pattern.pattern
				} else {
					searchStart = match.range.last + 1
				}
			}

			check(missingPatterns.isEmpty()) {
				buildString {
					appendLine("Structure rule failed: $rule")
					missingPatterns.forEach { appendLine("missing in order: $it") }
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
			":recommendation-contract" to emptySet(),
			":recommendation-core" to emptySet(),
			":application" to setOf(":recommendation-core"),
			":application-crawler" to setOf(":recommendation-core"),
			":infrastructure-jpa-core" to emptySet(),
			":infrastructure-jpa" to setOf(":application", ":persistence-model", ":infrastructure-jpa-core"),
			":infrastructure-jpa-crawler" to setOf(":application-crawler", ":persistence-model", ":infrastructure-jpa-core"),
			":infrastructure-security" to setOf(":application"),
			":web-app" to setOf(":application", ":infrastructure-jpa", ":infrastructure-security", ":recommendation-contract"),
			":crawler-job" to setOf(":application-crawler", ":infrastructure-jpa-crawler"),
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
			":recommendation-contract" to emptySet(),
			":recommendation-core" to setOf(":recommendation-contract"),
			":application" to setOf(":recommendation-contract"),
			":application-crawler" to setOf(":laptop-taxonomy", ":recommendation-contract"),
			":infrastructure-jpa-core" to emptySet(),
			":infrastructure-jpa" to emptySet(),
			":infrastructure-jpa-crawler" to emptySet(),
			":infrastructure-security" to emptySet(),
			":web-app" to emptySet(),
			":crawler-job" to emptySet(),
			":integration-tests" to emptySet(),
		).forEach { (projectPath, expectedProjectPaths) ->
			assertProjectDependencies(
				rule = "public module dependency graph must expose only shared contracts",
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
				":recommendation-contract",
			),
		)

		assertPathAbsent(
			rule = "ops must be the only tracked operations config surface",
			paths = listOf("nginx"),
		)

		assertPresent(
			rule = "deploy workflow must run PostgreSQL integration tests before bootJar",
			paths = listOf(".github/workflows/deploy-web.yml"),
			patterns = listOf(
				Regex("""services:\s+postgres:"""),
				Regex("""POSTGRES_INTEGRATION_TESTS:\s+"true""""),
				Regex("""POSTGRES_INTEGRATION_JDBC_URL:\s+jdbc:postgresql://127\.0\.0\.1:5432/laptopgg_test"""),
			),
		)

		assertPresent(
			rule = "crawler workflow must run PostgreSQL integration tests before production crawl",
			paths = listOf(".github/workflows/crawler.yml", "README.md", "docs/architecture.md", "ops/RUNBOOK.md"),
			patterns = listOf(
				Regex("""services:\s+postgres:"""),
				Regex("""POSTGRES_INTEGRATION_TESTS:\s+"true""""),
				Regex("""POSTGRES_INTEGRATION_JDBC_URL:\s+jdbc:postgresql://127\.0\.0\.1:5432/laptopgg_test"""),
				Regex(""":integration-tests:test --tests '\*Postgres\*'"""),
				Regex("""runs PostgreSQL integration tests against a local PostgreSQL service before opening the production write path"""),
				Regex("""크롤러 워크플로는 운영 DB 쓰기 전에 로컬 PostgreSQL 16 service DB로 PostgreSQL 통합 테스트를 먼저 실행합니다"""),
				Regex("""Crawler workflow validates the persistence path against a local PostgreSQL service before the production crawl"""),
			),
		)

		assertOrdered(
			rule = "crawler workflow must validate before opening the production database tunnel",
			path = ".github/workflows/crawler.yml",
			patterns = listOf(
				Regex("""name: Test and build crawler jar"""),
				Regex("""name: Prepare SSH key"""),
				Regex("""name: Open PostgreSQL tunnel"""),
				Regex("""name: Run crawler"""),
			),
		)

		assertPresent(
			rule = "deploy workflow must serialize releases so rollback can finish",
			paths = listOf(".github/workflows/deploy-web.yml", "docs/architecture.md", "ops/RUNBOOK.md"),
			patterns = listOf(
				Regex("""cancel-in-progress:\s+false"""),
				Regex("""Web deploy workflow runs are serialized"""),
				Regex("""Deploy workflow runs are serialized"""),
			),
		)

		assertPresent(
			rule = "PostgreSQL integration tests must cover recommendation ordering",
			paths = listOf("integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/PostgresRecommendationOrderingIntegrationTest.kt"),
			patterns = listOf(
				Regex("""POSTGRES_INTEGRATION_TESTS"""),
				Regex("""recommended database pages match calculator order"""),
				Regex("""RecommendationUseCase\.entries"""),
			),
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
			rule = "JPA entities must not own lifecycle timestamp defaults",
			paths = listOf("persistence-model/src/main"),
			patterns = listOf(
				Regex("""LocalDateTime\.now\(\)"""),
				Regex("""LocalDateTime::now"""),
			),
		)

		assertPresent(
			rule = "crawler run start time must be application owned and persisted explicitly",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunCommandFactory.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawlerRunJpaAdapter.kt",
				"application-crawler/src/test/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunCommandFactoryTest.kt",
				"infrastructure-jpa-crawler/src/test/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawlerRunJpaAdapterTest.kt",
			),
			patterns = listOf(
				Regex("""val startedAt: LocalDateTime"""),
				Regex("""startedAt = now\(\)"""),
				Regex("""val timestamp = now\(\)"""),
				Regex("""startedAt = command\.startedAt"""),
				Regex("""start command owns the run start time"""),
				Regex("""create stores application owned started time"""),
			),
		)

		assertPresent(
			rule = "crawler run observability must keep detail refresh and price-only counters through persistence",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSummary.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunCommandFactory.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawlerRunJpaAdapter.kt",
				"persistence-model/src/main/kotlin/going9/laptopgg/persistence/model/crawler/CrawlerRun.kt",
				"infrastructure-jpa-core/src/main/resources/db/migration/V10__crawler_run_observability_counts.sql",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerJobSummaryLogger.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/PostgresFlywayMigrationTest.kt",
				"ops/RUNBOOK.md",
			),
			patterns = listOf(
				Regex("""detailRefreshCount"""),
				Regex("""priceOnlyUpdatedCount"""),
				Regex("""detail_refresh_count"""),
				Regex("""price_only_updated_count"""),
				Regex("""ALTER TABLE public\.crawler_run"""),
				Regex("""detailRefreshCount=\{\} priceOnlyUpdatedCount=\{\}"""),
			),
		)

		assertPresent(
			rule = "crawler job runner must record fatal failures without swallowing fatal errors",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerJobExecutor.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerJobSummaryLogger.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunUseCase.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/runner/CrawlerJobExecutorTest.kt",
			),
			patterns = listOf(
				Regex("""catch \(exception: Exception\)"""),
				Regex("""catch \(failure: Throwable\)"""),
				Regex("""fun fail\(runId: Long, failure: Throwable\)"""),
				Regex("""fun logRunFailure\(runId: Long, request: CrawlerJobRequest, failure: Throwable\)"""),
				Regex("""if \(failure is Exception\)"""),
				Regex("""throw failure"""),
				Regex("""failure\.addSuppressed\(trackingFailure\)"""),
				Regex("""crawler fatal error is recorded and propagated"""),
				Regex("""crawler fatal error is propagated when failed run tracking fails"""),
			),
		)

		assertAbsent(
			rule = "crawler job runner must not use runCatching for recoverable job failures",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerJobExecutor.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerJobSummaryLogger.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunUseCase.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunCommandFactory.kt",
			),
			patterns = listOf(
				Regex("""runCatching"""),
			),
		)

		assertPresent(
			rule = "crawler detail fetch concurrency must be capped at the job boundary",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerJobProperties.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/runner/CrawlerStartupRunnerTest.kt",
				".github/workflows/crawler.yml",
				"ops/RUNBOOK.md",
			),
			patterns = listOf(
				Regex("""MAX_DETAIL_FETCH_CONCURRENCY = 12"""),
				Regex("""coerceAtMost\(MAX_DETAIL_FETCH_CONCURRENCY\)"""),
				Regex("""detail fetch concurrency is capped for operational safety"""),
				Regex("""CRAWLER_DETAIL_FETCH_CONCURRENCY.*12 or less"""),
				Regex("""Detail fetch concurrency is capped at 12"""),
			),
		)

		assertPresent(
			rule = "crawler filter profile must be typed after configuration parsing",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerFilterProfile.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerJobProperties.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerJobExecutor.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/DanawaCrawlSourceResolver.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunModels.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawlerRunJpaAdapter.kt",
			),
			patterns = listOf(
				Regex("""enum class CrawlerFilterProfile"""),
				Regex("""fun resolvedFilterProfile\(\): CrawlerFilterProfile"""),
				Regex("""val filterProfile: CrawlerFilterProfile"""),
				Regex("""override fun resolve\(filterProfile: CrawlerFilterProfile\)"""),
				Regex("""filterProfile = command\.filterProfile\.storageValue"""),
			),
		)

		assertPresent(
			rule = "crawler filter profile fallback must stay observable before execution",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerFilterProfile.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerJobProperties.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerStartupRunner.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/runner/CrawlerStartupRunnerTest.kt",
			),
			patterns = listOf(
				Regex("""data class CrawlerFilterProfileResolution"""),
				Regex("""usedDefaultForUnknownValue"""),
				Regex("""fun resolvedFilterProfileResolution\(\): CrawlerFilterProfileResolution"""),
				Regex("""logger\.warn\("""),
				Regex("""unknown filter profile keeps fallback observability"""),
			),
		)

		assertAbsent(
			rule = "crawler runtime must not pass raw filter profile strings after configuration parsing",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/runner",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/source/CrawlSourceResolver.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/DanawaCrawlSourceResolver.kt",
			),
			patterns = listOf(
				Regex("""filterProfileRaw"""),
				Regex("""rawProfile"""),
				Regex("""filterProfile: String"""),
				Regex("""DanawaFilterProfile"""),
			),
		)

		assertPresent(
			rule = "crawler service orchestration must be covered without disabled live smoke tests",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSourceRunner.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerServiceTest.kt",
			),
			patterns = listOf(
				Regex("""internal interface CrawlSourceRunUseCase"""),
				Regex(""": CrawlSourceRunUseCase"""),
				Regex("""private val crawlSourceRunner: CrawlSourceRunUseCase"""),
				Regex("""crawlAll traverses resolved sources with first requested page only once"""),
				Regex("""crawlAll stops traversing sources after the limit is reached"""),
			),
		)

		assertAbsent(
			rule = "crawler tests must not keep disabled live smoke placeholders",
			paths = listOf("crawler-job/src/test"),
			patterns = listOf(
				Regex("""@Disabled"""),
				Regex("""live crawling smoke test"""),
			),
		)

		assertAbsent(
			rule = "JPA entities must not be Kotlin data classes",
			paths = listOf("persistence-model/src/main"),
			patterns = listOf(
				Regex("""data\s+class\s+(CrawlerRun|Laptop|LaptopPriceHistory|LaptopProfile|LaptopUsage|RecommendationScore|Comment)\b"""),
			),
		)

		assertAbsent(
			rule = "JPA to-one associations must explicitly avoid default eager fetching",
			paths = listOf("persistence-model/src/main"),
			patterns = listOf(
				Regex("""@ManyToOne\s*$"""),
				Regex("""@ManyToOne\(\s*\)"""),
				Regex("""@OneToOne\s*$"""),
				Regex("""@OneToOne\(optional\s*="""),
			),
		)

		assertPresent(
			rule = "comment persistence must enforce required application fields",
			paths = listOf(
				"persistence-model/src/main/kotlin/going9/laptopgg/persistence/model/web/Comment.kt",
				"infrastructure-jpa-core/src/main/resources/db/migration/V9__comment_required_fields.sql",
			),
			patterns = listOf(
				Regex("""@JoinColumn\(name = "laptop_id", nullable = false\)"""),
				Regex("""@Column\(nullable = false\)\s+var author"""),
				Regex("""@Column\(nullable = false\)\s+var content"""),
				Regex("""@Column\(name = "pass_word", nullable = false\)"""),
				Regex("""comment_invalid_legacy"""),
				Regex("""ALTER COLUMN author SET NOT NULL"""),
				Regex("""ALTER COLUMN content SET NOT NULL"""),
				Regex("""ALTER COLUMN pass_word SET NOT NULL"""),
				Regex("""ALTER COLUMN laptop_id SET NOT NULL"""),
			),
		)

		assertPresent(
			rule = "comment read contracts must expose persisted non-null ids",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/comment/CommentModels.kt",
				"application/src/main/kotlin/going9/laptopgg/application/comment/port/CommentPort.kt",
				"web-app/src/main/kotlin/going9/laptopgg/web/dto/response/CommentResponse.kt",
				"infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web/CommentJpaAdapter.kt",
				"infrastructure-jpa/src/test/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web/CommentJpaAdapterStateTest.kt",
			),
			patterns = listOf(
				Regex("""data class CommentResult\(\s+val id: Long,"""),
				Regex("""data class CommentRecord\(\s+val id: Long,"""),
				Regex("""data class CommentResponse\(\s+val id: Long,"""),
				Regex("""Persisted comment id must not be null"""),
				Regex("""findById rejects persisted comment without generated id with explicit application error"""),
			),
		)

		assertAbsent(
			rule = "comment read contracts must not expose nullable ids",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/comment/CommentModels.kt",
				"application/src/main/kotlin/going9/laptopgg/application/comment/port/CommentPort.kt",
				"web-app/src/main/kotlin/going9/laptopgg/web/dto/response/CommentResponse.kt",
			),
			patterns = listOf(
				Regex("""val id: Long\?"""),
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

		assertPresent(
			rule = "web-facing application errors must be explicit and mapped at web boundary",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/common/ApplicationException.kt",
				"web-app/src/main/kotlin/going9/laptopgg/web/controller/WebExceptionHandler.kt",
				"web-app/src/main/resources/templates/error/application-error.html",
				"web-app/src/test/kotlin/going9/laptopgg/LaptopGgApplicationTests.kt",
				"web-app/src/test/kotlin/going9/laptopgg/web/controller/WebExceptionHandlerTest.kt",
				"application/src/test/kotlin/going9/laptopgg/application/comment/ManageCommentUseCaseTest.kt",
			),
			patterns = listOf(
				Regex("""sealed class ApplicationException"""),
				Regex("""class ResourceNotFoundException"""),
				Regex("""class InvalidCommandException"""),
				Regex("""class AuthenticationFailedException"""),
				Regex("""class ApplicationInvalidStateException"""),
				Regex("""@ControllerAdvice"""),
				Regex("""@ExceptionHandler\(ApplicationException::class\)"""),
				Regex("""HttpStatus\.NOT_FOUND"""),
				Regex("""HttpStatus\.BAD_REQUEST"""),
				Regex("""HttpStatus\.FORBIDDEN"""),
				Regex("""HttpStatus\.INTERNAL_SERVER_ERROR"""),
				Regex("""request\.requestURI\.startsWith\("/api/"\)"""),
				Regex("""ModelAndView"""),
				Regex("""error/application-error"""),
				Regex("""web api maps missing application resources to 404 response"""),
				Regex("""web api maps invalid application commands to 400 response"""),
				Regex("""web api maps invalid application state to 500 response"""),
				Regex("""web page maps missing application resources to html error page"""),
				Regex("""add rejects blank comment fields before persistence"""),
			),
		)

		assertAbsent(
			rule = "web-facing application flows must not throw generic argument exceptions",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/comment/ManageCommentUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/laptop/GetLaptopDetailUseCase.kt",
				"infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web/CommentJpaAdapter.kt",
				"infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web/LaptopDetailJpaAdapter.kt",
				"infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web/RecommendationCandidateJpaAdapter.kt",
			),
			patterns = listOf(
				Regex("""IllegalArgumentException"""),
				Regex("""require\("""),
				Regex("""requireNotNull"""),
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

		assertPresent(
			rule = "crawler application errors must be explicit instead of generic argument failures",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/common/CrawlerApplicationException.kt",
				"application-crawler/src/test/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopServiceTest.kt",
				"application-crawler/src/test/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunServiceTest.kt",
			),
			patterns = listOf(
				Regex("""sealed class CrawlerApplicationException"""),
				Regex("""class CrawlerResourceNotFoundException"""),
				Regex("""class CrawlerInvalidCommandException"""),
				Regex("""class CrawlerInvalidStateException"""),
				Regex("""saveListSnapshot rejects missing existing laptop with explicit crawler error"""),
				Regex("""finish rejects missing crawler run with explicit crawler error"""),
			),
		)

		assertPresent(
			rule = "crawler run persistence state and use case result must expose a persisted non-null id",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunModels.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawlerRunJpaAdapter.kt",
				"infrastructure-jpa-crawler/src/test/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawlerRunJpaAdapterTest.kt",
			),
			patterns = listOf(
				Regex("""data class CrawlerRunState\(\s+val id: Long,"""),
				Regex("""data class CrawlerRunRecord\(\s+val id: Long,"""),
				Regex("""CrawlerInvalidStateException"""),
				Regex("""Persisted crawler run id must not be null"""),
				Regex("""create rejects saved crawler run without generated id with explicit crawler error"""),
			),
		)

		assertAbsent(
			rule = "crawler job runner must rely on tracked run id contract instead of local null checks",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerJobExecutor.kt"),
			patterns = listOf(
				Regex("""requireNotNull\(crawlerRun\.id\)"""),
				Regex("""crawlerRun\.id!!"""),
			),
		)

		assertAbsent(
			rule = "crawler save run and score flows must not throw generic argument exceptions",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunService.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopPersistenceJpaAdapter.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/RecommendationScoreJpaAdapter.kt",
			),
			patterns = listOf(
				Regex("""IllegalArgumentException"""),
				Regex("""require\("""),
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

		assertPresent(
			rule = "crawler recommendation score command must keep use case typed until adapter storage",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreService.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/RecommendationScoreJpaAdapter.kt",
				"application-crawler/src/test/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreServiceTest.kt",
			),
			patterns = listOf(
				Regex("""val useCase: RecommendationUseCase"""),
				Regex("""useCase = useCase,"""),
				Regex("""val useCase = command\.useCase\.name"""),
				Regex("""containsExactlyElementsOf\(RecommendationUseCase\.entries\)"""),
			),
		)

		assertAbsent(
			rule = "crawler recommendation score application contract must not use raw use case strings",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreService.kt",
			),
			patterns = listOf(
				Regex("""val useCase: String"""),
				Regex("""useCase = useCase\.name"""),
			),
		)

		assertAbsent(
			rule = "crawler profile port must not expose JPA entities",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/port/CrawledLaptopProfilePort.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/LaptopProfileService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreService.kt",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.persistence\.model\.laptop\.LaptopProfile"""),
				Regex("""LaptopProfile\("""),
			),
		)

		assertAbsent(
			rule = "profile score composer must delegate metrics and use-case weights",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/ProfileScorePolicy.kt"),
			patterns = listOf(
				Regex("""fun\s+(portabilityScore|portabilityTier|batteryCapacityScore|batteryTier|ramScore|displayScore|tgpScore|usageBoosts|clampScore)\b"""),
				Regex("""RESOLUTION_REGEX"""),
				Regex("""REFERENCE_PIXELS"""),
				Regex("""roundToInt"""),
				Regex("""(cpu|gpu)\.(performanceScore|lowPowerScore|creatorBonus)\s*\*"""),
				Regex("""\*\s*0\.[0-9]+"""),
			),
		)

		assertAbsent(
			rule = "profile metric policy must delegate display scoring",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/ProfileMetricPolicy.kt"),
			patterns = listOf(
				Regex("""fun\s+displayScore\b"""),
				Regex("""RESOLUTION_REGEX"""),
				Regex("""REFERENCE_PIXELS"""),
				Regex("""brightnessScore"""),
				Regex("""refreshScore"""),
				Regex("""roundToInt"""),
			),
		)

		assertAbsent(
			rule = "profile metric policy must delegate mobility and battery scoring",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/ProfileMetricPolicy.kt"),
			patterns = listOf(
				Regex("""fun\s+(portabilityScore|portabilityTier|batteryCapacityScore|batteryTier)\b"""),
				Regex("""PortabilityTier\.(TABLET_LIGHT|ULTRALIGHT|LIGHT|BALANCED|HEAVY|UNKNOWN)"""),
				Regex("""BatteryTier\.(VERY_HIGH|HIGH|MEDIUM|LOW|VERY_LOW|UNKNOWN)"""),
			),
		)

		assertAbsent(
			rule = "crawler laptop persistence boundary must not expose JPA entities",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/port/CrawledLaptopPersistencePort.kt",
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

		assertAbsent(
			rule = "gpu classifier must delegate model catalog data",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/GpuClassifier.kt"),
			patterns = listOf(
				Regex("""RTX 5090"""),
				Regex("""RADEON 890M"""),
				Regex("""ADRENO X2-90"""),
				Regex("""DISCRETE_GPU_KEYWORDS"""),
				Regex("""INTEGRATED_GPU_KEYWORDS"""),
				Regex("""normalized\.contains"""),
			),
		)

		assertAbsent(
			rule = "gpu model catalog must delegate rule model and integrated discrete keyword lookup",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/GpuModelCatalog.kt"),
			patterns = listOf(
				Regex("""data class GpuClassificationRule"""),
				Regex("""DISCRETE_GPU_KEYWORDS"""),
				Regex("""INTEGRATED_GPU_KEYWORDS"""),
				Regex("""fun\s+is(Discrete|Integrated)Model\b"""),
			),
		)

		assertAbsent(
			rule = "cpu classifier must delegate token resolution",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/CpuClassifier.kt"),
			patterns = listOf(
				Regex("""fun\s+resolveCpuToken\b"""),
				Regex("""fun\s+normalizeCpuToken\b"""),
				Regex("""productName\.uppercase"""),
				Regex("""MACBOOK"""),
				Regex("""맥북"""),
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
			rule = "crawler laptop change detector must delegate field-level change policy",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/CrawledLaptopChangeDetector.kt"),
			patterns = listOf(
				Regex("""fun\s+changed(Text|Present|Usages)\b"""),
				Regex("""newValue\?\.trim\(\)"""),
				Regex("""currentUsages\.sorted\(\)"""),
				Regex("""listOf\(\s*$"""),
			),
		)

		assertAbsent(
			rule = "crawler save use case must avoid nested transactional service entrypoints",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopService.kt"),
			patterns = listOf(
				Regex("""laptopProfileService\.syncProfile\("""),
				Regex("""laptopPriceHistoryService\.recordCurrentPrice\("""),
			),
		)

		assertAbsent(
			rule = "crawler save use case must delegate post-save synchronization",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopService.kt"),
			patterns = listOf(
				Regex("""LaptopProfileService"""),
				Regex("""LaptopPriceHistoryService"""),
				Regex("""syncProfileInTransaction"""),
				Regex("""recordCurrentPriceInTransaction"""),
			),
		)

		assertAbsent(
			rule = "crawler save use case must delegate existing laptop lookup loading",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopService.kt"),
			patterns = listOf(
				Regex("""findAllByProductCodes"""),
				Regex("""findAllByDetailPages"""),
				Regex("""fun\s+PersistedCrawledLaptopSnapshot\.toExistingSnapshot"""),
				Regex("""ExistingCrawledLaptopLookup\("""),
			),
		)

		assertPathAbsent(
			rule = "crawler profile backfill surface must not remain without an explicit runner",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/port/CrawledLaptopProfileSourcePort.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopProfileSourceJpaAdapter.kt",
			),
		)

		assertAbsent(
			rule = "crawler profile service must only own single-laptop synchronization",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/LaptopProfileService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/port/CrawledLaptopProfilePort.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/repository/crawler",
			),
			patterns = listOf(
				Regex("""syncMissingProfiles"""),
				Regex("""syncIncompleteProfiles"""),
				Regex("""PROFILE_BACKFILL_BATCH_SIZE"""),
				Regex("""findIdsWithoutProfile"""),
				Regex("""findAllWithUsageByIdIn"""),
				Regex("""findLaptopIdsWithIncompleteStaticScores"""),
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
			rule = "crawler run tracking service must delegate command creation and storage formatting",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunService.kt"),
			patterns = listOf(
				Regex("""CreateCrawlerRunCommand\("""),
				Regex("""UpdateCrawlerRunCommand\("""),
				Regex("""LocalDateTime\.now"""),
				Regex("""fun\s+CrawlerRunCompletionStatus\.toStatusResult\b"""),
				Regex("""fun\s+List<String>\.toStorageText\b"""),
				Regex("""fun\s+String\.truncateForStorage\b"""),
				Regex("""MAX_STORED_SAMPLES"""),
				Regex("""MAX_TEXT_LENGTH"""),
			),
		)

		assertAbsent(
			rule = "crawler application persistence services must use injected time providers",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/price/LaptopPriceHistoryService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreService.kt",
			),
			patterns = listOf(
				Regex("""LocalDateTime\.now\(\)"""),
			),
		)

		assertPresent(
			rule = "crawler application persistence service tests must pin generated timestamps",
			paths = listOf(
				"application-crawler/src/test/kotlin/going9/laptopgg/application/crawler/price/LaptopPriceHistoryServiceTest.kt",
				"application-crawler/src/test/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreServiceTest.kt",
			),
			patterns = listOf(
				Regex("""now = \{ fixedNow }"""),
				Regex("""capturedAt\)\.isEqualTo\(fixedNow\)"""),
				Regex("""updatedAt\)\.isEqualTo\(fixedNow\)"""),
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
			rule = "recommendation-contract must stay a Spring-free public recommendation vocabulary module",
			paths = listOf("recommendation-contract/src/main", "recommendation-contract/build.gradle.kts"),
			patterns = listOf(
				Regex("""RecommendationScoringPolicy"""),
				Regex("""RecommendationScoreInputs"""),
				Regex("""RecommendationGateInputs"""),
				Regex("""going9\.laptopgg\.application"""),
				Regex("""going9\.laptopgg\.persistence\.model"""),
				Regex("""going9\.laptopgg\.infrastructure"""),
				Regex("""going9\.laptopgg\.web"""),
				Regex("""org\.springframework"""),
				Regex("""spring-boot"""),
				Regex("""spring-context"""),
				Regex("""project\("""),
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
			rule = "crawler laptop persistence JPA adapter must delegate entity mapping",
			paths = listOf("infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopPersistenceJpaAdapter.kt"),
			patterns = listOf(
				Regex("""=\s*Laptop\("""),
				Regex("""LaptopUsage\("""),
				Regex("""fun\s+CrawledLaptopCommand\.toLaptop"""),
				Regex("""fun\s+Laptop\.applyUpdate"""),
			),
		)

		assertAbsent(
			rule = "crawler profile JPA adapter must delegate entity mapping and field updates",
			paths = listOf("infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopProfileJpaAdapter.kt"),
			patterns = listOf(
				Regex("""LaptopProfile\("""),
				Regex("""LaptopProfileSnapshot\("""),
				Regex("""fun\s+LaptopProfile\.applySnapshot"""),
				Regex("""fun\s+LaptopProfile\.toState"""),
				Regex("""fun\s+<T>\s+updateField"""),
			),
		)

		assertPresent(
			rule = "crawler JPA mappers must reject missing persisted ids with explicit crawler state errors",
			paths = listOf(
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopSnapshotMapper.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopProfileEntityMapper.kt",
				"infrastructure-jpa-crawler/src/test/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopMapperStateTest.kt",
			),
			patterns = listOf(
				Regex("""CrawlerInvalidStateException"""),
				Regex("""laptop snapshot mapper rejects entity without persisted id with explicit crawler error"""),
				Regex("""profile mapper rejects laptop reference without persisted id with explicit crawler error"""),
			),
		)

		assertAbsent(
			rule = "crawler JPA mappers must not use generic null assertions for persisted ids",
			paths = listOf(
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopSnapshotMapper.kt",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/crawler/CrawledLaptopProfileEntityMapper.kt",
			),
			patterns = listOf(
				Regex("""requireNotNull"""),
			),
		)

		assertAbsent(
			rule = "application command and result contracts must not expose persistence models",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/common",
				"application/src/main/kotlin/going9/laptopgg/application/comment/CommentModels.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence",
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
			rule = "production source sets must not expose test fixture factories",
			paths = listOf(
				"application/src/main",
				"application-crawler/src/main",
				"recommendation-contract/src/main",
				"recommendation-core/src/main",
				"laptop-taxonomy/src/main",
				"persistence-model/src/main",
				"infrastructure-jpa/src/main",
				"infrastructure-jpa-crawler/src/main",
				"infrastructure-jpa-core/src/main",
				"infrastructure-security/src/main",
				"web-app/src/main",
				"crawler-job/src/main",
			),
			patterns = listOf(
				Regex("""fun\s+fixture\s*\("""),
			),
		)

		assertPathAbsent(
			rule = "crawler persistence contracts must be split by command snapshot lookup and result",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/CrawlerPersistenceModels.kt"),
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

		assertPresent(
			rule = "application recommendation candidate query must keep use case typed until adapter storage",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/port/RecommendationCandidatePort.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendLaptopsUseCase.kt",
				"infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter/web/RecommendationCandidateJpaAdapter.kt",
			),
			patterns = listOf(
				Regex("""val useCase: RecommendationUseCase"""),
				Regex("""useCase = useCase,"""),
				Regex("""useCase = query\.useCase\.name"""),
			),
		)

		assertAbsent(
			rule = "application recommendation candidate contract must not use raw use case strings",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/port/RecommendationCandidatePort.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendLaptopsUseCase.kt",
			),
			patterns = listOf(
				Regex("""val useCase: String"""),
				Regex("""useCase = useCase\.name"""),
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
				Regex("""RecommendationScoringPolicy"""),
				Regex("""project\(":persistence-model"\)"""),
				Regex("""project\(":application-crawler"\)"""),
				Regex("""project\(":recommendation-core"\)"""),
				Regex("""project\(":infrastructure-jpa-crawler"\)"""),
			),
		)

		assertPresent(
			rule = "recommendation API pagination must stay bounded for low-resource runtime",
			paths = listOf(
				"web-app/src/main/kotlin/going9/laptopgg/web/controller/PageQueryAdapter.kt",
				"web-app/src/test/kotlin/going9/laptopgg/web/controller/RecommendationControllerTest.kt",
			),
			patterns = listOf(
				Regex("""MAX_PAGE_SIZE = 100"""),
				Regex("""recommend api keeps pagination within operational bounds"""),
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

		assertPresent(
			rule = "crawler runtime must not own production schema migrations",
			paths = listOf(
				"crawler-job/src/main/resources/application.yml",
				"crawler-job/src/test/kotlin/going9/laptopgg/CrawlerJobProductionProfileTests.kt",
				".github/workflows/crawler.yml",
				"README.md",
				"docs/architecture.md",
				"ops/RUNBOOK.md",
			),
			patterns = listOf(
				Regex("""on-profile:\s+crawler"""),
				Regex("""flyway:\s+enabled:\s+false"""),
				Regex("""SPRING_FLYWAY_ENABLED:\s+"false""""),
				Regex("""CrawlerJobProductionProfileTests"""),
				Regex("""FlywayMigrationInitializer"""),
				Regex("""crawler job은 운영 DB에서 Flyway migration을 실행하지 않습니다"""),
				Regex("""crawler-job` does not run Flyway migrations against production"""),
				Regex("""crawler workflow sets `SPRING_FLYWAY_ENABLED=false`"""),
			),
		)

		assertPresent(
			rule = "web runtime must use graceful shutdown within systemd stop budget",
			paths = listOf(
				"web-app/src/main/resources/application.yml",
				"web-app/src/test/kotlin/going9/laptopgg/LaptopGgApplicationTests.kt",
				"docs/architecture.md",
				"ops/RUNBOOK.md",
				"ops/env/laptopgg.env.example",
				"ops/systemd/laptopgg.service",
			),
			patterns = listOf(
				Regex("""server:\s+shutdown:\s+graceful"""),
				Regex("""timeout-per-shutdown-phase:"""),
				Regex("""web runtime uses graceful shutdown settings"""),
				Regex("""SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=20s"""),
				Regex("""TimeoutStopSec=30"""),
				Regex("""graceful shutdown"""),
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
			rule = "web-app implementation packages must not expose public Kotlin declarations",
			paths = listOf(
				"web-app/src/main/kotlin/going9/laptopgg/web/config",
				"web-app/src/main/kotlin/going9/laptopgg/web/controller",
				"web-app/src/main/kotlin/going9/laptopgg/web/view",
			),
			patterns = listOf(
				Regex("""^(data\s+)?class\s+"""),
				Regex("""^object\s+"""),
				Regex("""^interface\s+"""),
				Regex("""^enum\s+class\s+"""),
				Regex("""^fun\s+"""),
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

		assertPresent(
			rule = "laptop detail page must expose the comment use case surface it loads",
			paths = listOf(
				"web-app/src/main/kotlin/going9/laptopgg/web/controller/LaptopPageController.kt",
				"web-app/src/main/kotlin/going9/laptopgg/web/controller/CommentPageController.kt",
				"web-app/src/main/resources/templates/laptop-detail.html",
				"web-app/src/test/kotlin/going9/laptopgg/web/controller/CommentPageControllerTest.kt",
				"web-app/src/test/kotlin/going9/laptopgg/web/controller/LaptopDetailPageRenderingTest.kt",
			),
			patterns = listOf(
				Regex("""CommentRequest\(laptopId = laptopId\)"""),
				Regex("""@PostMapping\("/comments/\{commentId}/delete"\)"""),
				Regex("""commentsOfLaptop"""),
				Regex("""th:action="@\{/comments}""""),
				Regex("""comment\.id"""),
				Regex("""'/edit'"""),
				Regex("""'/delete'"""),
				Regex("""comment delete redirects to laptop detail after service call"""),
				Regex("""laptop detail page renders comment create list edit and delete surface"""),
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

		assertPathAbsent(
			rule = "crawler application use case config must be split by responsibility",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerApplicationUseCaseConfig.kt"),
		)

		assertPathAbsent(
			rule = "crawler profile resolver config must not be named as a use case config",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerProfileUseCaseConfig.kt"),
		)

		assertAbsent(
			rule = "runtime configs must delegate crawler application object assembly",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerProfileUseCaseConfig.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerProfileResolverConfig.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerPersistenceUseCaseConfig.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/config/CrawlerRunUseCaseConfig.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/config/IntegrationCrawlerUseCaseConfig.kt",
			),
			patterns = listOf(
				Regex("""application\.crawler\.price\.LaptopPriceHistoryService"""),
				Regex("""application\.crawler\.profile\.LaptopProfileService"""),
				Regex("""application\.crawler\.recommendation\.RecommendationScoreService"""),
				Regex("""return\s+CpuTokenResolver\("""),
				Regex("""return\s+CpuClassifier\("""),
				Regex("""return\s+GpuClassifier\("""),
				Regex("""return\s+ProfileScorePolicy\("""),
				Regex("""return\s+CrawledCpuManufacturerResolver\("""),
				Regex("""return\s+CrawledCpuModelResolver\("""),
				Regex("""return\s+CrawledGraphicsModelResolver\("""),
				Regex("""return\s+LaptopProfileFactory\("""),
				Regex("""return\s+RecommendationScoreService\("""),
				Regex("""return\s+LaptopProfileService\("""),
				Regex("""return\s+LaptopPriceHistoryService\("""),
				Regex("""return\s+SaveCrawledLaptopService\("""),
				Regex("""return\s+TrackCrawlerRunService\("""),
				Regex("""return\s+CrawlerRunLockService\("""),
				Regex("""CrawledLaptopPostSaveSynchronizer\("""),
				Regex("""ExistingCrawledLaptopLookupLoader\("""),
			),
		)

		assertPathAbsent(
			rule = "crawler application object assemblers must be split by feature responsibility",
			paths = listOf("application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/assembly/CrawlerUseCaseAssembler.kt"),
		)

		assertAbsent(
			rule = "crawler application persistence implementations must stay behind use case contracts",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/SaveCrawledLaptopService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/CrawledLaptopPostSaveSynchronizer.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/ExistingCrawledLaptopLookupLoader.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/CrawledLaptopChangeDetector.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/persistence/CrawledLaptopFieldChangePolicy.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/price/LaptopPriceHistoryService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/LaptopProfileService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/recommendation/RecommendationScoreService.kt",
			),
			patterns = listOf(
				Regex("""^(data\s+)?class\s+"""),
				Regex("""^object\s+"""),
				Regex("""^enum\s+class\s+"""),
			),
		)

		assertAbsent(
			rule = "crawler run implementations must stay behind use case contracts",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/TrackCrawlerRunService.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunLockUseCase.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/run/CrawlerRunCommandFactory.kt",
			),
			patterns = listOf(
				Regex("""^(data\s+)?class\s+"""),
				Regex("""^object\s+"""),
				Regex("""^enum\s+class\s+"""),
			),
		)

		assertAbsent(
			rule = "crawler profile scoring implementations must stay inside application-crawler",
			paths = listOf(
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/BatteryMetricPolicy.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/CpuClassifier.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/CpuTokenResolver.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/DisplayMetricPolicy.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/GpuClassifier.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/LaptopProfileFactory.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/MobilityMetricPolicy.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/ProfileMetricPolicy.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/ProfileScorePolicy.kt",
				"application-crawler/src/main/kotlin/going9/laptopgg/application/crawler/profile/ProfileUseCaseScorePolicy.kt",
			),
			patterns = listOf(
				Regex("""^(data\s+)?class\s+"""),
				Regex("""^object\s+"""),
				Regex("""^interface\s+"""),
				Regex("""^enum\s+class\s+"""),
			),
		)

		assertAbsent(
			rule = "crawler-job must not use root package default component scan",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/CrawlerJobApplication.kt"),
			patterns = listOf(
				Regex("""@SpringBootApplication\s*$"""),
				Regex(""""going9\.laptopgg\.job","""),
				Regex(""""going9\.laptopgg\.job\.crawler","""),
			),
		)

		assertAbsent(
			rule = "web-app must not use root package default component scan",
			paths = listOf("web-app/src/main/kotlin/going9/laptopgg/LaptopGgApplication.kt"),
			patterns = listOf(
				Regex("""@SpringBootApplication\s*$"""),
				Regex(""""going9\.laptopgg\.infrastructure\.jpa","""),
				Regex(""""going9\.laptopgg\.infrastructure\.jpa\.repository\.crawler","""),
				Regex(""""going9\.laptopgg\.web","""),
			),
		)

		assertAbsent(
			rule = "runtime applications must import infrastructure adapter config facades instead of scanning adapter packages",
			paths = listOf(
				"web-app/src/main/kotlin/going9/laptopgg/LaptopGgApplication.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/CrawlerJobApplication.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/InfrastructureJpaTestApplication.kt",
			),
			patterns = listOf(
				Regex(""""going9\.laptopgg\.infrastructure\.jpa\.adapter"""),
				Regex(""""going9\.laptopgg\.infrastructure\.security""""),
				Regex("""EnableConfigurationProperties\(PasswordHashProperties::class\)"""),
				Regex("""import going9\.laptopgg\.infrastructure\.jpa\.config\.(Web|Crawler)JpaRepositoryConfig"""),
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
			rule = "infrastructure adapter implementation packages must not expose public Kotlin declarations",
			paths = listOf(
				"infrastructure-jpa/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter",
				"infrastructure-jpa-crawler/src/main/kotlin/going9/laptopgg/infrastructure/jpa/adapter",
				"infrastructure-security/src/main/kotlin/going9/laptopgg/infrastructure/security/BcryptPasswordHashAdapter.kt",
			),
			patterns = listOf(
				Regex("""^(data\s+)?class\s+"""),
				Regex("""^object\s+"""),
				Regex("""^interface\s+"""),
				Regex("""^enum\s+class\s+"""),
			),
		)

		assertAbsent(
			rule = "Danawa endpoint client must not own low-level HTTP retry or pacing",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaClient.kt"),
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
			rule = "Danawa endpoint client must delegate HTTP request construction",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaClient.kt"),
			patterns = listOf(
				Regex("""java\.net\.URI"""),
				Regex("""java\.net\.http\.HttpRequest"""),
				Regex("""URLEncoder"""),
				Regex("""BodyPublishers"""),
				Regex("""USER_AGENT"""),
				Regex("""LIST_AJAX_URL"""),
				Regex("""PRODUCT_DESCRIPTION_URL"""),
				Regex("""FORM_URLENCODED"""),
				Regex("""fun\s+buildFormData"""),
			),
		)

		assertAbsent(
			rule = "Danawa URL constants must stay centralized in the endpoint catalog",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/list",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/source",
			),
			patterns = listOf(
				Regex("""https://prod\.danawa\.com"""),
				Regex("""getProductList\.ajax\.php"""),
				Regex("""getProductDescription\.ajax\.php"""),
			),
		)

		assertAbsent(
			rule = "Danawa HTTP transport must delegate retry policy and request pacing",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaHttpClient.kt"),
			patterns = listOf(
				Regex("""ThreadLocalRandom"""),
				Regex("""requestPacingLock"""),
				Regex("""nextAllowedRequestAtMillis"""),
				Regex("""globalCooldownUntilMillis"""),
				Regex("""RETRYABLE_STATUS_CODES"""),
				Regex("""MAX_HTTP_RETRIES"""),
				Regex("""fun\s+(awaitRequestSlot|extendGlobalCooldown|retryDelayMillis|randomJitterMillis)\b"""),
			),
		)

		assertPresent(
			rule = "Danawa HTTP client must expose explicit provider transport and error contracts",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaHttpTransport.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaHttpException.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaHttpClient.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaClient.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaHttpClientTest.kt",
			),
			patterns = listOf(
				Regex("""internal fun interface DanawaHttpTransport"""),
				Regex("""internal class JavaDanawaHttpTransport"""),
				Regex("""sealed class DanawaHttpException"""),
				Regex("""class DanawaHttpStatusException"""),
				Regex("""class DanawaHttpRequestException"""),
				Regex("""class DanawaHttpInterruptedException"""),
				Regex("""transport\.send\(request\)"""),
				Regex("""catch \(e: DanawaHttpException\)"""),
				Regex("""send rejects non retryable status with explicit Danawa http status error"""),
			),
		)

		assertAbsent(
			rule = "Danawa retrying HTTP client must not own Java HTTP transport or generic state errors",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaHttpClient.kt"),
			patterns = listOf(
				Regex("""HttpClient\.newBuilder"""),
				Regex("""HttpResponse\.BodyHandlers"""),
				Regex("""IllegalStateException"""),
			),
		)

		assertPresent(
			rule = "Danawa pacing and retry policies must expose deterministic timing seams",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaTiming.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaRequestPacer.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaRetryPolicy.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaRequestPacerTest.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaRetryPolicyTest.kt",
			),
			patterns = listOf(
				Regex("""internal fun interface DanawaClock"""),
				Regex("""internal fun interface DanawaSleeper"""),
				Regex("""internal fun interface DanawaJitterSource"""),
				Regex("""clock\.currentTimeMillis\(\)"""),
				Regex("""sleeper\.sleep\(waitMillis\)"""),
				Regex("""jitterSource\.nextLong"""),
				Regex("""awaitRequestSlot sleeps until the next paced request slot"""),
				Regex("""uses status-specific bounded retry delay"""),
			),
		)

		assertAbsent(
			rule = "Danawa pacing and retry policies must not call system time sleep or random directly",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaRequestPacer.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/client/DanawaRetryPolicy.kt",
			),
			patterns = listOf(
				Regex("""System\.currentTimeMillis"""),
				Regex("""Thread\.sleep"""),
				Regex("""ThreadLocalRandom"""),
				Regex("""fun\s+randomJitterMillis"""),
			),
		)

		assertAbsent(
			rule = "CrawlerService must delegate source page traversal",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""DanawaListPageCrawler"""),
				Regex("""CrawlProductBatchProcessor"""),
				Regex("""DanawaListParser"""),
				Regex("""ProductCard"""),
				Regex("""DuplicateTailStopPolicy"""),
				Regex("""fetchProductPageBatch"""),
				Regex("""while\s*\(currentPage"""),
				Regex("""currentPage\+\+"""),
			),
		)

		assertPresent(
			rule = "crawler source traversal must expose deterministic page timing",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlTiming.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSourceRunner.kt",
			),
			patterns = listOf(
				Regex("""internal fun interface CrawlClock"""),
				Regex("""internal class SystemCrawlClock"""),
				Regex("""crawlClock\.currentTimeMillis\(\)"""),
			),
		)

		assertAbsent(
			rule = "crawler source runner must not call system time directly",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSourceRunner.kt"),
			patterns = listOf(
				Regex("""System\.currentTimeMillis"""),
			),
		)

		assertAbsent(
			rule = "Danawa detail html parser must delegate scalar spec value parsing",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaDetailParser.kt"),
			patterns = listOf(
				Regex("""fun\s+(parseScreenSize|parseIntValue|parseDoubleValue|parseWeightValue|parseCapacityInGb|parseCountValue|parsePossible|parseThunderboltCount|parseUsbCCount|parseSdCard|normalizeOs|normalizeCpuManufacturer)\b"""),
				Regex("""roundToInt"""),
				Regex("""USB-C겸용"""),
			),
		)

		assertPresent(
			rule = "Danawa detail crawler must keep product exceptions separate from fatal errors",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaDetailCrawler.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaDetailCrawlerFailureContractTest.kt",
			),
			patterns = listOf(
				Regex("""catch \(exception: Exception\)"""),
				Regex("""error = exception"""),
				Regex("""detail fetch exception is returned as product failure outcome"""),
				Regex("""fatal detail fetch error is not downgraded to product failure outcome"""),
			),
		)

		assertPresent(
			rule = "detail fetch executor must unwrap worker failures before the job boundary",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/detail/DetailFetchExecutor.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/detail/DetailFetchExecutorTest.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaDetailCrawlerFailureContractTest.kt",
			),
			patterns = listOf(
				Regex("""ExecutionException"""),
				Regex("""throw exception\.cause \?: exception"""),
				Regex("""fatal detail task error is rethrown without execution wrapper"""),
				Regex("""\.isSameAs\(error\)"""),
			),
		)

		assertAbsent(
			rule = "Danawa detail crawler must not catch fatal throwables as product failures",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaDetailCrawler.kt"),
			patterns = listOf(
				Regex("""runCatching"""),
				Regex("""Throwable"""),
				Regex("""IllegalStateException"""),
			),
		)

		assertAbsent(
			rule = "Danawa detail parsing and snapshot merging must not use generic null assertions",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaDetailParser.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaSummaryFallbackParser.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/LaptopSnapshotMerger.kt",
			),
			patterns = listOf(
				Regex("""!!"""),
				Regex("""requireNotNull"""),
			),
		)

		assertAbsent(
			rule = "Danawa scalar value parser must not instantiate application profile policies",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaSpecValueParser.kt"),
			patterns = listOf(
				Regex("""CrawledCpuManufacturerResolver"""),
				Regex("""normalizeCpuManufacturer"""),
			),
		)

		assertAbsent(
			rule = "laptop snapshot merger must delegate CPU manufacturer resolution",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/LaptopSnapshotMerger.kt"),
			patterns = listOf(
				Regex("""fun\s+resolveCpuManufacturer\b"""),
				Regex("""normalizeCpuManufacturer"""),
				Regex("""SNAPDRAGON"""),
				Regex("""애플\(ARM\)"""),
				Regex("""퀄컴"""),
			),
		)

		assertAbsent(
			rule = "Danawa detail html parser must delegate summary fallback parsing",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaDetailParser.kt"),
			patterns = listOf(
				Regex("""fun\s+extractSummaryText\b"""),
				Regex("""fun\s+parseSummaryFallback\b"""),
				Regex("""fun\s+isEmpty\b"""),
				Regex("""summary_info"""),
				Regex("""SummaryFallback\("""),
			),
		)

		assertAbsent(
			rule = "Danawa list html parser must delegate request context extraction",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/list/DanawaListParser.kt"),
			patterns = listOf(
				Regex("""fun\s+extractListRequestContext\b"""),
				Regex("""fun\s+extractJsScalar\b"""),
				Regex("""ListRequestContext\("""),
				Regex("""nPriceCompareListCount"""),
				Regex("""sPriceCompareListType"""),
			),
		)

		assertPathAbsent(
			rule = "Danawa list html parser must be split by product cards and page metadata",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/list/DanawaListParser.kt"),
		)

		assertAbsent(
			rule = "Danawa product card parser must not own page metadata parsing",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/list/DanawaProductCardParser.kt"),
			patterns = listOf(
				Regex("""num_nav_wrap"""),
				Regex("""totalProductCount"""),
				Regex("""가격비교"""),
				Regex("""movePage"""),
				Regex("""ListPageMetadata"""),
			),
		)

		assertAbsent(
			rule = "Danawa list page metadata parser must not own product card parsing",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/list/DanawaListPageMetadataParser.kt"),
			patterns = listOf(
				Regex("""ProductCard\("""),
				Regex("""prod_item"""),
				Regex("""prod_pricelist"""),
				Regex("""thumb_image"""),
				Regex("""normalizeDetailPage"""),
			),
		)

		assertAbsent(
			rule = "list request context must not own Danawa HTTP form serialization",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/list/ListModels.kt"),
			patterns = listOf(
				Regex("""fun\s+toFormData"""),
				Regex("""searchAttributeValue\[\]"""),
				Regex("""sDiscountProductRate"""),
				Regex("""BodyPublishers"""),
			),
		)

		assertAbsent(
			rule = "list request model must not own Danawa defaults or endpoints",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/list/ListModels.kt"),
			patterns = listOf(
				Regex("""DanawaEndpoints"""),
				Regex("""NOTEBOOK_LIST_URL"""),
				Regex("=\\s*\"758\""),
				Regex("=\\s*\"SAVEASC\""),
			),
		)

		assertPathAbsent(
			rule = "Danawa HTTP client implementation must not live in the generic crawler client package",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/client",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/client",
			),
		)

		assertAbsent(
			rule = "generic crawler list and detail packages must not own Danawa provider implementations",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/list",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/detail",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/list",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/detail",
			),
			patterns = listOf(
				Regex("""Danawa"""),
				Regex("""DetailRequestContext"""),
				Regex("""ParsedSpecTable"""),
				Regex("""SummaryFallback"""),
			),
		)

		assertAbsent(
			rule = "Danawa list html parser must not own crawl page diagnostics",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/list/DanawaListParser.kt"),
			patterns = listOf(
				Regex("""fun\s+createPageSignature\b"""),
				Regex("""(?m)^\s+fun\s+extractQueryParam\b"""),
			),
		)

		assertAbsent(
			rule = "crawl source runner must delegate page diagnostics logging",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSourceRunner.kt"),
			patterns = listOf(
				Regex("""페이지 진단"""),
				Regex("""페이지 처리 시간"""),
				Regex("""AJAX 페이지네이션"""),
				Regex("""visiblePagesLog"""),
				Regex("""stableHash"""),
				Regex("""describeCard"""),
			),
		)

		assertAbsent(
			rule = "crawl page diagnostics logger must delegate diagnostic field formatting",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlPageDiagnosticsLogger.kt"),
			patterns = listOf(
				Regex("""ProductPageSignature\.stableHash"""),
				Regex("""fun\s+(visiblePages|describeCard|extractQueryParam)\b"""),
				Regex("""nextPageHint\s*\?:"""),
				Regex("""priceCompareCount\s*\?:"""),
				Regex("""data class CrawlPageDiagnosticContext"""),
			),
		)

		assertAbsent(
			rule = "crawl source runner must delegate traversal state and page freshness analysis",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSourceRunner.kt"),
			patterns = listOf(
				Regex("""seenPageSignatures"""),
				Regex("""var\s+consecutiveDuplicateOnlyPages"""),
				Regex("""var\s+currentPage"""),
				Regex("""ProductPageSignature\.create"""),
				Regex("""seenDetailPages\.add"""),
				Regex("""currentPage\+\+"""),
				Regex("""priceCompareCount\s*\n\s*\?\.takeIf"""),
			),
		)

		assertAbsent(
			rule = "crawl source runner must delegate source stop policy",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSourceRunner.kt"),
			patterns = listOf(
				Regex("""DuplicateTailStopPolicy"""),
				Regex("""expectedLastPage\s*!=\s*null"""),
				Regex("""!\s*pageBatch\.hasNextPage"""),
			),
		)

		assertAbsent(
			rule = "crawl source runner must delegate stop decision logging",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSourceRunner.kt"),
			patterns = listOf(
				Regex("""CrawlSourceStopReason"""),
				Regex("""logDuplicateTailStop"""),
				Regex("""fun\s+logStopDecision\b"""),
				Regex("""총 상품 수 기준"""),
				Regex("""다음 페이지가 없어"""),
			),
		)

		assertAbsent(
			rule = "crawl product batch processor must delegate detail refresh planning",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlProductBatchProcessor.kt"),
			patterns = listOf(
				Regex("""DetailRefreshPolicy"""),
				Regex("""DetailRefreshWorkItem\("""),
				Regex("""mutableListOf<DetailRefreshWorkItem>"""),
				Regex("""existingLookup\.find"""),
			),
		)

		assertAbsent(
			rule = "crawl product batch processor must delegate snapshot saving and detail outcome handling",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlProductBatchProcessor.kt"),
			patterns = listOf(
				Regex("""DetailRefreshOutcome\b"""),
				Regex("""BuildLaptopResult"""),
				Regex("""fun\s+saveListSnapshot"""),
				Regex("""saveCrawledLaptopUseCase\.saveListSnapshot\("""),
				Regex("""saveCrawledLaptopUseCase\.saveOrUpdateLaptop\("""),
				Regex("""recordDegraded"""),
				Regex("""recordFailure"""),
				Regex("""logger\.(warn|error)"""),
			),
		)

		assertAbsent(
			rule = "recommend laptop use case must delegate filtering, sorting, and result mapping",
			paths = listOf("application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendLaptopsUseCase.kt"),
			patterns = listOf(
				Regex("""ScreenSizeMode\."""),
				Regex("""RecommendationCandidateFilter\("""),
				Regex("""SortProperty\."""),
				Regex("""fun\s+(manufacturerName|resolutionLabel|minimumRoundedAverageTotal|resolveSortMode)\b"""),
				Regex("""RESOLUTION_REGEX"""),
				Regex("""ceil\("""),
			),
		)

		assertAbsent(
			rule = "recommendation score calculator must delegate display reason selection",
			paths = listOf("application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendationScoreCalculator.kt"),
			patterns = listOf(
				Regex("""fun\s+buildReasons\b"""),
				Regex("""문서 작업에 잘 맞아요"""),
				Regex("""사진·영상 작업에 잘 맞아요"""),
				Regex("""sortedByDescending"""),
			),
		)

		assertAbsent(
			rule = "recommendation page model factory must delegate presentation catalog",
			paths = listOf("web-app/src/main/kotlin/going9/laptopgg/web/view/RecommendationPageModelFactory.kt"),
			patterns = listOf(
				Regex("""UseCaseOption\("""),
				Regex("""ScreenSizeModeOption\("""),
				Regex("""fun\s+(useCaseOptions|screenSizeModeOptions|useCaseLabel|useCaseHeading|screenSizeSummary|budgetPresetList|weightPresetList)\b"""),
			),
		)

		assertAbsent(
			rule = "runtime configs must delegate recommendation use case assembly",
			paths = listOf(
				"web-app/src/main/kotlin/going9/laptopgg/web/config/WebApplicationUseCaseConfig.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/config/IntegrationWebUseCaseConfig.kt",
			),
			patterns = listOf(
				Regex("""RecommendationReasonBuilder\("""),
				Regex("""RecommendationScoreCalculator\("""),
				Regex("""RecommendationCandidateFilterFactory\("""),
				Regex("""RecommendationSortModeResolver\("""),
				Regex("""LaptopRecommendationResultMapper\("""),
			),
		)

		assertPresent(
			rule = "recommendation use case must own application read transaction boundary",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendLaptopsUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendationUseCaseAssembler.kt",
				"web-app/src/main/kotlin/going9/laptopgg/web/config/WebApplicationUseCaseConfig.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/config/IntegrationWebUseCaseConfig.kt",
				"application/src/test/kotlin/going9/laptopgg/application/recommendation/RecommendLaptopsUseCaseTransactionTest.kt",
			),
			patterns = listOf(
				Regex("""private val transactionPort: ApplicationTransactionPort"""),
				Regex("""transactionPort\.read"""),
				Regex("""transactionPort: ApplicationTransactionPort"""),
				Regex("""transactionPort = transactionPort"""),
				Regex("""recommendation query runs inside application read transaction"""),
			),
		)

		assertAbsent(
			rule = "runtime configs must delegate web application use case assembly",
			paths = listOf("web-app/src/main/kotlin/going9/laptopgg/web/config/WebApplicationUseCaseConfig.kt"),
			patterns = listOf(
				Regex("""return\s+ManageCommentUseCase\("""),
				Regex("""return\s+GetLaptopDetailUseCase\("""),
				Regex("""return\s+RecommendLaptopsUseCase\("""),
			),
		)

		assertAbsent(
			rule = "application use case constructors must stay hidden behind assemblers",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/comment/ManageCommentUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/laptop/GetLaptopDetailUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendLaptopsUseCase.kt",
			),
			patterns = listOf(
				Regex("""^class\s+DefaultManageCommentUseCase\("""),
				Regex("""^class\s+DefaultGetLaptopDetailUseCase\("""),
				Regex("""^class\s+DefaultRecommendLaptopsUseCase\("""),
			),
		)

		assertPresent(
			rule = "application use case contracts must be public interfaces with internal implementations",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/comment/ManageCommentUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/laptop/GetLaptopDetailUseCase.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendLaptopsUseCase.kt",
			),
			patterns = listOf(
				Regex("""interface\s+ManageCommentUseCase"""),
				Regex("""internal\s+class\s+DefaultManageCommentUseCase"""),
				Regex("""interface\s+GetLaptopDetailUseCase"""),
				Regex("""internal\s+class\s+DefaultGetLaptopDetailUseCase"""),
				Regex("""interface\s+RecommendLaptopsUseCase"""),
				Regex("""internal\s+class\s+DefaultRecommendLaptopsUseCase"""),
			),
		)

		assertAbsent(
			rule = "recommendation application implementation classes must not expose public Kotlin declarations",
			paths = listOf(
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendationCandidateFilterFactory.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendationReasonBuilder.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendationScoreCalculator.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/RecommendationSortModeResolver.kt",
				"application/src/main/kotlin/going9/laptopgg/application/recommendation/LaptopRecommendationResultMapper.kt",
			),
			patterns = listOf(
				Regex("""^(data\s+)?class\s+"""),
				Regex("""^object\s+"""),
				Regex("""^interface\s+"""),
				Regex("""^enum\s+class\s+"""),
			),
		)

		assertPathAbsent(
			rule = "recommendation page presentation must be split by use case screen size and preset responsibility",
			paths = listOf("web-app/src/main/kotlin/going9/laptopgg/web/view/RecommendationPagePresentation.kt"),
		)

		assertAbsent(
			rule = "recommendation use case presentation must not own screen size or preset catalogs",
			paths = listOf("web-app/src/main/kotlin/going9/laptopgg/web/view/RecommendationUseCasePresentation.kt"),
			patterns = listOf(
				Regex("""ScreenSizeMode"""),
				Regex("""LaptopRecommendationRequest"""),
				Regex("""budgetPresets"""),
				Regex("""weightPresets"""),
			),
		)

		assertAbsent(
			rule = "recommendation screen size presentation must not own use case or preset catalogs",
			paths = listOf("web-app/src/main/kotlin/going9/laptopgg/web/view/RecommendationScreenSizePresentation.kt"),
			patterns = listOf(
				Regex("""RecommendationUseCase"""),
				Regex("""UseCaseOption"""),
				Regex("""budgetPresets"""),
				Regex("""weightPresets"""),
			),
		)

		assertAbsent(
			rule = "crawler startup runner must delegate job execution",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerStartupRunner.kt"),
			patterns = listOf(
				Regex("""CrawlerRunLockUseCase"""),
				Regex("""TrackCrawlerRunUseCase"""),
				Regex("""CrawlerService"""),
				Regex("""CrawlerRunCompletionStatus"""),
				Regex("""CrawlerRunSummary"""),
				Regex("""runLocked"""),
				Regex("""CRAWLER_SUMMARY"""),
			),
		)

		assertAbsent(
			rule = "crawler job executor must delegate operational summary logging",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/runner/CrawlerJobExecutor.kt"),
			patterns = listOf(
				Regex("""LoggerFactory"""),
				Regex("""logger\."""),
				Regex("""CRAWLER_SUMMARY"""),
				Regex("""Crawler degraded samples"""),
				Regex("""Crawler failure samples"""),
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

		assertAbsent(
			rule = "PostgreSQL integration tests must use shared database support",
			paths = listOf(
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/PostgresCrawlerPersistenceUseCaseTest.kt",
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/PostgresFlywayMigrationTest.kt",
			),
			patterns = listOf(
				Regex("""PostgreSQLContainer"""),
				Regex("""DockerImageName"""),
				Regex("""POSTGRES_INTEGRATION_JDBC_URL"""),
				Regex("""POSTGRES_INTEGRATION_USERNAME"""),
				Regex("""POSTGRES_INTEGRATION_PASSWORD"""),
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
				"integration-tests/src/test/kotlin/going9/laptopgg/integration/recommendation/PostgresRecommendationOrderingIntegrationTest.kt",
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
			rule = "crawler-job implementation packages must not expose public Kotlin declarations",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job"),
			patterns = listOf(
				Regex("""^(data\s+)?class\s+"""),
				Regex("""^object\s+"""),
				Regex("""^interface\s+"""),
				Regex("""^enum\s+class\s+"""),
			),
		)

		assertAbsent(
			rule = "crawler orchestration must not own filter profile source mapping",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt"),
			patterns = listOf(
				Regex("""DanawaCrawlSourceResolver"""),
				Regex("""fun\s+resolveFilterProfile"""),
				Regex("""fun\s+resolveCrawlSources"""),
				Regex("""CrawlerFilterSets"""),
				Regex("""APPLE_MACBOOK_LIST_URL"""),
			),
		)

		assertPathAbsent(
			rule = "Danawa attribute filter catalog must not live in the generic source package",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/source/CrawlerSet.kt"),
		)

		assertAbsent(
			rule = "generic crawl source package must not own Danawa attribute filter codes",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/source"),
			patterns = listOf(
				Regex("""going9\.laptopgg\.job\.crawler\.danawa"""),
				Regex("""Danawa(Endpoints|AttributeFilterCatalog|CrawlSourceResolver|FilterProfile)"""),
				Regex("""CrawlerFilterSets"""),
				Regex("""\d+\|6492\|"""),
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
			rule = "crawl progress must delegate sample formatting and capping",
			paths = listOf("crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlProgress.kt"),
			patterns = listOf(
				Regex("""mutableListOf<String>\("""),
				Regex("""fun\s+recordSample\b"""),
				Regex("""productCard\.productCode.*productCard\.productName"""),
				Regex("""samples\.size\s*>=\s*maxSampleCount"""),
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
				Regex("""DanawaDetailCrawler"""),
				Regex("""DetailRefreshWorkItem"""),
				Regex("""DetailRefreshOutcome"""),
				Regex("""loadExistingLookup"""),
				Regex("""saveListSnapshot"""),
				Regex("""saveOrUpdateLaptop"""),
			),
		)

		assertAbsent(
			rule = "crawler orchestration must depend on provider-neutral crawler ports",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/orchestration",
			),
			patterns = listOf(
				Regex("""going9\.laptopgg\.job\.crawler\.danawa"""),
				Regex("""Danawa(ListPageCrawler|DetailCrawler|CrawlSourceResolver)"""),
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

		assertPresent(
			rule = "crawler detail fetch concurrency must be wrapped behind an explicit lifecycle",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/detail/DetailFetchExecutor.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/DetailFetchExecutorFactory.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/detail/DetailFetchExecutorTest.kt",
			),
			patterns = listOf(
				Regex("""internal class DetailFetchExecutor"""),
				Regex("""Closeable"""),
				Regex("""awaitTermination"""),
				Regex("""shutdownNow"""),
				Regex("""internal fun interface DetailFetchExecutorFactory"""),
				Regex("""DetailFetchExecutor\.fixed\(crawlerJobProperties\.resolvedDetailFetchConcurrency\(\)\)"""),
				Regex("""detailFetchExecutorFactory\.create\(\)\.use"""),
				Regex("""fetch executes detail tasks and preserves work item order"""),
				Regex("""close shuts down detail task executor lifecycle"""),
			),
		)

		assertAbsent(
			rule = "crawler detail fetch executor service must not leak through orchestration ports",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/detail/ProductDetailCrawler.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlerService.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlSourceRunner.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlProductBatchProcessor.kt",
			),
			patterns = listOf(
				Regex("""java\.util\.concurrent\.ExecutorService"""),
				Regex("""java\.util\.concurrent\.Executors"""),
				Regex("""Executors\.newFixedThreadPool"""),
				Regex("""\.shutdown\(\)"""),
			),
		)

		assertAbsent(
			rule = "crawler detail refresh planning and snapshot timestamps must use explicit time inputs",
			paths = listOf(
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/danawa/detail/LaptopSnapshotMerger.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/detail/DetailRefreshPolicy.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/DetailRefreshPlanner.kt",
				"crawler-job/src/main/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlProductBatchProcessor.kt",
			),
			patterns = listOf(
				Regex("""LocalDateTime\.now\(\)"""),
				Regex("""needsRefresh\(existingLaptop\)"""),
			),
		)

		assertPresent(
			rule = "crawler detail timestamp tests must pin crawl time",
			paths = listOf(
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/danawa/detail/DanawaCrawlerNormalizationTest.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/orchestration/DetailRefreshPlannerTest.kt",
				"crawler-job/src/test/kotlin/going9/laptopgg/job/crawler/orchestration/CrawlProductBatchProcessorTest.kt",
			),
			patterns = listOf(
				Regex("""now = \{ fixedNow }"""),
				Regex("""lastDetailedCrawledAt\)\.isEqualTo\(fixedNow\)"""),
				Regex("""now = fixedNow"""),
				Regex("""fixedNow\.minusDays\(45\)"""),
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
