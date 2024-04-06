package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class Manufacturer(

    val afterService: AfterService,

    @OneToMany(mappedBy = "manufacturer")
    val laptops: MutableList<LaptopManufacturer>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}

enum class AfterService {
    TOP,
    GOOD,
    SOSO,
    BAD,
}