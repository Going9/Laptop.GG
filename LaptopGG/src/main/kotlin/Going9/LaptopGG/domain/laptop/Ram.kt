package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class Ram(

    val capacity: Int,

    @OneToMany(mappedBy = "ram")
    val laptops: MutableList<LaptopRam>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}