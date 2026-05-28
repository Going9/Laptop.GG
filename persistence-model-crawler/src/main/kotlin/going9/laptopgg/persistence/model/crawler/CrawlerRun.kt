package going9.laptopgg.persistence.model.crawler

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "crawler_run")
class CrawlerRun(
    @Column(name = "filter_profile", nullable = false)
    var filterProfile: String,

    @Column(name = "start_page", nullable = false)
    var startPage: Int,

    @Column(name = "limit_count")
    var limitCount: Int?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CrawlerRunStatus = CrawlerRunStatus.RUNNING,

    @Column(name = "processed_count", nullable = false)
    var processedCount: Int = 0,

    @Column(name = "created_count", nullable = false)
    var createdCount: Int = 0,

    @Column(name = "updated_count", nullable = false)
    var updatedCount: Int = 0,

    @Column(name = "detail_refresh_count", nullable = false)
    var detailRefreshCount: Int = 0,

    @Column(name = "price_only_updated_count", nullable = false)
    var priceOnlyUpdatedCount: Int = 0,

    @Column(name = "degraded_count", nullable = false)
    var degradedCount: Int = 0,

    @Column(name = "failed_count", nullable = false)
    var failedCount: Int = 0,

    @Column(name = "failure_samples", columnDefinition = "text")
    var failureSamples: String? = null,

    @Column(name = "error_message", columnDefinition = "text")
    var errorMessage: String? = null,

    @Column(name = "started_at", nullable = false)
    var startedAt: LocalDateTime,

    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)

enum class CrawlerRunStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
    SKIPPED_LOCKED,
}
