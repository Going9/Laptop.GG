package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopRam(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    @ManyToOne
    @JoinColumn(name = "ram_id")
    val ram: Ram,

    val slot: Int? = null,  // if ram upgrade possible, input count of ram slot

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}