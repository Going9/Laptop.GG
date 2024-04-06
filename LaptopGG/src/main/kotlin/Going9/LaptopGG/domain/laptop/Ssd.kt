package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class Ssd(

    val capacity: Int,

    @OneToMany(mappedBy = "ssd")
    val laptops: MutableList<LaptopSsd>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}