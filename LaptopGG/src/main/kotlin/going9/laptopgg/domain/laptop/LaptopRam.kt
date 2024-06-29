package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopRam(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    val capacity: Int,

    val slot: Int? = null,  // if ram upgrade possible, input count of ram slot

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}