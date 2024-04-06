package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class Cpu(

    val name: String,

    val highPowerUsage: Boolean,

    val isIntel: Boolean,

    @OneToMany(mappedBy = "cpu")
    val laptops: MutableList<LaptopCpu>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    ) {
}