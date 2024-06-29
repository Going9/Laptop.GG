package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopSsd(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    val capacity: Int,

    val slot: Int? = null,  // if ssd upgrade possible, input count of ssd slot

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}