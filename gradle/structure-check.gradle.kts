import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

val verifyStructure by tasks.registering {
    group = "verification"
    description = "Verifies module dependency boundaries."

    doLast {
        fun directDependencies(projectPath: String, configuration: String) =
            project(projectPath).configurations.findByName(configuration)?.dependencies
                ?.withType(ProjectDependency::class.java)?.map { it.dependencyProject.path }?.toSet() ?: emptySet()

        fun resolvedProjects(projectPath: String, configuration: String) =
            project(projectPath).configurations.findByName(configuration)?.incoming?.resolutionResult?.allComponents
                ?.mapNotNull { it.id as? ProjectComponentIdentifier }?.map { it.projectPath }
                ?.filterNot { it == projectPath }?.toSet() ?: emptySet()

        fun resolvedModules(projectPath: String, configuration: String) =
            project(projectPath).configurations.findByName(configuration)?.incoming?.resolutionResult?.allComponents
                ?.mapNotNull { it.id as? ModuleComponentIdentifier }?.map { "${it.group}:${it.module}" }?.toSet() ?: emptySet()

        fun assertDependencies(rule: String, projectPath: String, configuration: String, expected: Set<String>) =
            check(directDependencies(projectPath, configuration) == expected) {
                "$rule: $projectPath $configuration must be $expected; was ${directDependencies(projectPath, configuration)}"
            }

        fun assertResolvedProjects(rule: String, projectPath: String, configuration: String, expected: Set<String>) =
            check(resolvedProjects(projectPath, configuration) == expected) {
                "$rule: $projectPath $configuration must be $expected; was ${resolvedProjects(projectPath, configuration)}"
            }

        fun assertModulesAbsent(rule: String, projectPath: String, configuration: String, forbidden: Set<String>) =
            check(resolvedModules(projectPath, configuration).intersect(forbidden).isEmpty()) {
                "$rule: found ${resolvedModules(projectPath, configuration).intersect(forbidden)}"
            }

        mapOf(
            ":laptop-taxonomy" to emptySet(), ":persistence-model" to emptySet(),
            ":persistence-model-web" to emptySet(), ":persistence-model-crawler" to emptySet(),
            ":recommendation-contract" to emptySet(), ":recommendation-core" to emptySet(),
            ":application" to setOf(":recommendation-core"),
            ":application-crawler" to setOf(":recommendation-core"),
            ":infrastructure-jpa-core" to emptySet(), ":infrastructure-flyway" to emptySet(),
            ":infrastructure-jpa" to setOf(":application", ":persistence-model", ":persistence-model-web", ":infrastructure-jpa-core"),
            ":infrastructure-jpa-crawler" to setOf(":application-crawler", ":persistence-model", ":persistence-model-crawler", ":infrastructure-jpa-core"),
            ":infrastructure-security" to setOf(":application"),
            ":web-app" to setOf(":application", ":infrastructure-jpa", ":infrastructure-security", ":recommendation-contract"),
            ":crawler-job" to setOf(":application-crawler", ":infrastructure-jpa-crawler"), ":integration-tests" to emptySet(),
        ).forEach { (projectPath, expected) ->
            assertDependencies("runtime module dependency graph must stay top-down", projectPath, "implementation", expected)
        }

        mapOf(
            ":laptop-taxonomy" to emptySet(), ":persistence-model" to setOf(":laptop-taxonomy"),
            ":persistence-model-web" to setOf(":persistence-model"), ":persistence-model-crawler" to setOf(":persistence-model"),
            ":recommendation-contract" to emptySet(), ":recommendation-core" to setOf(":recommendation-contract"),
            ":application" to setOf(":recommendation-contract"),
            ":application-crawler" to setOf(":laptop-taxonomy", ":recommendation-contract"),
            ":infrastructure-jpa-core" to emptySet(), ":infrastructure-flyway" to emptySet(),
            ":infrastructure-jpa" to emptySet(), ":infrastructure-jpa-crawler" to emptySet(),
            ":infrastructure-security" to emptySet(), ":web-app" to emptySet(), ":crawler-job" to emptySet(),
            ":integration-tests" to emptySet(),
        ).forEach { (projectPath, expected) ->
            assertDependencies("public module dependency graph must expose only shared contracts", projectPath, "api", expected)
        }

        assertDependencies("integration tests may compose persistence and application modules", ":integration-tests", "testImplementation", setOf(
            ":application", ":application-crawler", ":laptop-taxonomy", ":persistence-model", ":persistence-model-web",
            ":persistence-model-crawler", ":infrastructure-jpa", ":infrastructure-jpa-core", ":infrastructure-jpa-crawler",
            ":recommendation-core", ":recommendation-contract",
        ))
        assertResolvedProjects("web runtime classpath must contain only web runtime project modules", ":web-app", "runtimeClasspath", setOf(
            ":application", ":recommendation-contract", ":recommendation-core", ":infrastructure-jpa", ":infrastructure-jpa-core",
            ":persistence-model", ":laptop-taxonomy", ":persistence-model-web", ":infrastructure-security", ":infrastructure-flyway",
        ))
        assertResolvedProjects("crawler runtime classpath must contain only crawler runtime project modules", ":crawler-job", "runtimeClasspath", setOf(
            ":application-crawler", ":laptop-taxonomy", ":recommendation-contract", ":recommendation-core", ":infrastructure-jpa-crawler",
            ":persistence-model", ":persistence-model-crawler", ":infrastructure-jpa-core",
        ))
        assertModulesAbsent("web runtime classpath must not carry crawler-only libraries", ":web-app", "runtimeClasspath", setOf("org.jsoup:jsoup"))
        assertModulesAbsent("crawler runtime classpath must stay non-web and migration-free", ":crawler-job", "runtimeClasspath", setOf(
            "org.springframework.boot:spring-boot-starter-web", "org.springframework.boot:spring-boot-starter-tomcat",
            "org.apache.tomcat.embed:tomcat-embed-core", "org.springframework:spring-webmvc", "org.thymeleaf:thymeleaf-spring6",
            "org.flywaydb:flyway-core", "org.flywaydb:flyway-database-postgresql",
        ))
        mapOf(":web-app" to setOf(":infrastructure-flyway"), ":crawler-job" to emptySet(), ":integration-tests" to setOf(":infrastructure-flyway")).forEach { (projectPath, expected) ->
            assertDependencies("Flyway migration resources must stay web-owned at runtime", projectPath, if (projectPath == ":integration-tests") "testRuntimeOnly" else "runtimeOnly", expected)
        }
    }
}
