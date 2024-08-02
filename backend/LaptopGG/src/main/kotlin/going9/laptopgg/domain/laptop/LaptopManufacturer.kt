package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopManufacturer(

    @OneToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    val service: ServiceRank,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}

enum class ServiceRank {
    TOP,
    GOOD,
    SOSO,
    BAD,
}