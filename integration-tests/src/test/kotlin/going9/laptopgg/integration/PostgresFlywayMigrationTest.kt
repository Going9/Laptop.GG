package going9.laptopgg.integration

import going9.laptopgg.integration.support.PostgresIntegrationDatabase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.sql.DriverManager

@EnabledIfEnvironmentVariable(named = "POSTGRES_INTEGRATION_TESTS", matches = "true")
class PostgresFlywayMigrationTest {

    @Test
    fun `flyway migrations apply to an empty postgres database`() {
        val flyway = Flyway.configure()
            .dataSource(
                PostgresIntegrationDatabase.jdbcUrl,
                PostgresIntegrationDatabase.username,
                PostgresIntegrationDatabase.password,
            )
            .locations("classpath:db/migration")
            .baselineOnMigrate(false)
            .cleanDisabled(false)
            .load()

        flyway.clean()
        flyway.migrate()

        DriverManager.getConnection(
            PostgresIntegrationDatabase.jdbcUrl,
            PostgresIntegrationDatabase.username,
            PostgresIntegrationDatabase.password,
        ).use { connection ->
            val tableNames = connection.metaData.getTables(null, "public", "%", arrayOf("TABLE")).use { tables ->
                generateSequence {
                    if (tables.next()) tables.getString("TABLE_NAME") else null
                }.toSet()
            }

            assertThat(tableNames).contains(
                "laptop",
                "laptop_profile",
                "laptop_usage",
                "laptop_price_history",
                "crawler_run",
                "recommendation_score",
            )

            connection.prepareStatement(
                """
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'crawler_run'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val crawlerRunColumns = generateSequence {
                        if (resultSet.next()) resultSet.getString("column_name") else null
                    }.toSet()

                    assertThat(crawlerRunColumns).contains(
                        "filter_profile",
                        "status",
                        "processed_count",
                        "detail_refresh_count",
                        "price_only_updated_count",
                        "failure_samples",
                        "started_at",
                        "ended_at",
                    )
                }
            }

            connection.prepareStatement(
                """
                select conname, convalidated
                from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                join pg_namespace n on n.oid = t.relnamespace
                where n.nspname = 'public'
                  and t.relname = 'laptop'
                  and c.contype = 'c'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val constraints = generateSequence {
                        if (resultSet.next()) resultSet.getString("conname") to resultSet.getBoolean("convalidated") else null
                    }.toMap()

                    assertThat(constraints).containsKeys(
                        "chk_laptop_name_required",
                        "chk_laptop_image_url_required",
                        "chk_laptop_detail_page_required",
                    )
                    assertThat(constraints["chk_laptop_name_required"]).isFalse()
                    assertThat(constraints["chk_laptop_image_url_required"]).isFalse()
                    assertThat(constraints["chk_laptop_detail_page_required"]).isFalse()
                }
            }

            assertThatThrownBy {
                connection.prepareStatement(
                    """
                    insert into public.laptop (name, image_url, detail_page)
                    values ('', 'https://example.com/laptop.jpg', 'https://example.com/detail')
                    """.trimIndent(),
                ).use { statement -> statement.executeUpdate() }
            }.hasMessageContaining("chk_laptop_name_required")

            assertThatThrownBy {
                connection.prepareStatement(
                    """
                    insert into public.laptop (name, image_url, detail_page)
                    values ('Valid Laptop', '   ', 'https://example.com/detail')
                    """.trimIndent(),
                ).use { statement -> statement.executeUpdate() }
            }.hasMessageContaining("chk_laptop_image_url_required")

            assertThatThrownBy {
                connection.prepareStatement(
                    """
                    insert into public.laptop (name, image_url, detail_page)
                    values ('Valid Laptop', 'https://example.com/laptop.jpg', null)
                    """.trimIndent(),
                ).use { statement -> statement.executeUpdate() }
            }.hasMessageContaining("chk_laptop_detail_page_required")

            connection.prepareStatement(
                """
                select conname, convalidated
                from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                join pg_namespace n on n.oid = t.relnamespace
                where n.nspname = 'public'
                  and t.relname = 'laptop_usage'
                  and c.contype = 'c'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val constraints = generateSequence {
                        if (resultSet.next()) resultSet.getString("conname") to resultSet.getBoolean("convalidated") else null
                    }.toMap()

                    assertThat(constraints).containsEntry("chk_laptop_usage_value_required", false)
                }
            }

            val validLaptopId = connection.prepareStatement(
                """
                insert into public.laptop (name, image_url, detail_page)
                values ('Valid Laptop', 'https://example.com/laptop.jpg', 'https://example.com/detail')
                returning id
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getLong("id")
                }
            }

            assertThatThrownBy {
                connection.prepareStatement(
                    """
                    insert into public.laptop_usage (laptop_id, laptop_usage)
                    values (?, ' ')
                    """.trimIndent(),
                ).use { statement ->
                    statement.setLong(1, validLaptopId)
                    statement.executeUpdate()
                }
            }.hasMessageContaining("chk_laptop_usage_value_required")

            connection.prepareStatement(
                """
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'recommendation_score'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val recommendationScoreColumns = generateSequence {
                        if (resultSet.next()) resultSet.getString("column_name") else null
                    }.toSet()

                    assertThat(recommendationScoreColumns).contains(
                        "laptop_id",
                        "use_case",
                        "gate_score",
                        "static_score",
                        "budget_weight",
                        "updated_at",
                    )
                }
            }

            connection.prepareStatement(
                """
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'comment'
                  and is_nullable = 'NO'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val requiredCommentColumns = generateSequence {
                        if (resultSet.next()) resultSet.getString("column_name") else null
                    }.toSet()

                    assertThat(requiredCommentColumns).contains(
                        "id",
                        "author",
                        "content",
                        "pass_word",
                        "laptop_id",
                    )
                }
            }

            connection.prepareStatement(
                """
                select column_name
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'comment_invalid_legacy'
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val invalidCommentColumns = generateSequence {
                        if (resultSet.next()) resultSet.getString("column_name") else null
                    }.toSet()

                    assertThat(invalidCommentColumns).contains(
                        "original_comment_id",
                        "reason",
                        "archived_at",
                    )
                }
            }
        }
    }
}
