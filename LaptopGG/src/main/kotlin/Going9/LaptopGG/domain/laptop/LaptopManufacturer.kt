package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopManufacturer(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    @ManyToOne
    @JoinColumn(name = "manufacturer_id")
    val manufacturer: Manufacturer,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}