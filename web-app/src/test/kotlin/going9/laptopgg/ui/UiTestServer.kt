package going9.laptopgg.ui

import going9.laptopgg.LaptopGgApplication
import going9.laptopgg.recommendation.RecommendationUseCase
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

// Test classpath only: production jars contain neither fixtures nor this entry point.
fun main() {
    SpringApplicationBuilder(LaptopGgApplication::class.java, UiFixtureConfiguration::class.java)
        .run(
            "--spring.profiles.active=test",
            "--server.address=127.0.0.1", "--server.port=18080",
            "--spring.datasource.url=jdbc:h2:mem:laptopgg-ui;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE",
            "--spring.datasource.driver-class-name=org.h2.Driver",
            "--spring.datasource.username=sa", "--spring.datasource.password=",
            "--spring.jpa.hibernate.ddl-auto=create-drop", "--spring.flyway.enabled=false",
            "--spring.task.scheduling.enabled=false",
        )
}

@TestConfiguration(proxyBeanMethods = false)
class UiFixtureConfiguration {
    @Bean
    fun uiFixtures(dataSource: DataSource) = ApplicationRunner {
        dataSource.connection.use { connection ->
            check(connection.metaData.url.startsWith("jdbc:h2:mem:laptopgg-ui"))
            fun insert(sql: String, vararg values: Any?) {
                connection.prepareStatement(sql).use { statement ->
                    values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
                    statement.executeUpdate()
                }
            }
            for (id in 1..26) {
                val missing = id == 26
                val name = if (id == 1) "테스트 노트북 긴 제품명 문서 작업과 이동을 위한 가벼운 모델 2026 한글EnglishLongProductNameWithoutSpaces" else "테스트 노트북 %02d".format(id)
                insert(
                    """insert into laptop (id, name, image_url, detail_page, price, cpu, cpu_manufacturer,
                        os, screen_size, resolution, ram_size, ram_type, graphics_type, weight, battery_capacity, storage_capacity, price_checked_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)""",
                    id, name, if (missing) "" else "/ui/laptop.svg", "/laptops/$id",
                    if (missing) null else 800000 + id * 10000, if (missing) null else "Core Ultra 5",
                    "인텔", if (missing) null else "Windows 11", if (missing) null else 14,
                    if (missing) null else "1920×1200", if (missing) null else 16, "LPDDR5X",
                    if (missing) null else "Intel Graphics", if (missing) null else 1.0 + id * 0.01,
                    if (missing) null else 60.0, if (missing) null else 512,
                )
                insert(
                    """insert into laptop_profile (laptop_id, cpu_class, gpu_class, battery_tier, portability_tier,
                        office_score, battery_score, casual_game_score, online_game_score, aaa_game_score, creator_score,
                        cpu_performance_score, low_power_cpu_score, gpu_performance_score, gpu_creator_bonus,
                        portability_score, display_score, ram_score, tgp_score)
                        values (?, 'UNKNOWN', 'UNKNOWN', 'UNKNOWN', 'UNKNOWN', 90, 90, 80, 80, 80, 80, 80, 90, 70, 0, 90, 80, 80, 0)""", id,
                )
                RecommendationUseCase.entries.forEach { useCase ->
                    insert(
                        """insert into recommendation_score (laptop_id, use_case, gate_score, static_score, budget_weight, updated_at)
                            values (?, ?, 95, 75, 0.2, timestamp '2026-01-01 00:00:00')""", id, useCase.name,
                    )
                }
            }
            connection.createStatement().use { statement ->
                statement.executeQuery("select count(*) from laptop").use { result ->
                    check(result.next() && result.getInt(1) == 26)
                }
            }
        }
    }
}
