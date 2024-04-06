package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class Gpu(
    val name: String,

    @OneToMany(mappedBy = "gpu")
    val laptops: MutableList<LaptopGpu>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}