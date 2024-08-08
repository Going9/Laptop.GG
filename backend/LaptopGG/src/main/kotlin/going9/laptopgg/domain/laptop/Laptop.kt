package going9.laptopgg.domain.laptop

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
    val laptopScreen: MutableList<LaptopScreen>,

    val manufacturer: String,
    val weight: Int,
    val thunderVolt: Int? = null,
    val usb4: Int? = null,
    val battery: Int,
    val name: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
