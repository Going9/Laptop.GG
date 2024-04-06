package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class Laptop(

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val laptopCpus: MutableList<LaptopCpu>,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val laptopGpus: MutableList<LaptopGpu>,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val laptopRams: MutableList<LaptopRam>,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val laptopSsds: MutableList<LaptopSsd>,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val laptopManufacturers: MutableList<LaptopManufacturer>,

    val weight: String,

    val thunderVolt: Int? = null,

    val battery: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}