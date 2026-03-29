package going9.laptopgg.domain.laptop

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "laptop_price_history")
data class LaptopPriceHistory(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laptop_id", nullable = false)
    val laptop: Laptop,

    val price: Int,

    val capturedAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
