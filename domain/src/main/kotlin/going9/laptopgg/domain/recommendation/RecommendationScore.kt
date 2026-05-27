package going9.laptopgg.domain.recommendation

import going9.laptopgg.domain.laptop.Laptop
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "recommendation_score",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_recommendation_score_laptop_use_case",
            columnNames = ["laptop_id", "use_case"],
        ),
    ],
)
class RecommendationScore(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laptop_id", nullable = false)
    var laptop: Laptop,

    @Column(name = "use_case", nullable = false)
    var useCase: String,

    @Column(name = "gate_score", nullable = false)
    var gateScore: Int,

    @Column(name = "static_score", nullable = false)
    var staticScore: Double,

    @Column(name = "budget_weight", nullable = false)
    var budgetWeight: Double,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
