package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopSsd(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    @ManyToOne
    @JoinColumn(name = "ssd_id")
    val ssd: Ssd,

    val slot: Int? = null,  // if ssd upgrade possible, input count of ssd slot

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}