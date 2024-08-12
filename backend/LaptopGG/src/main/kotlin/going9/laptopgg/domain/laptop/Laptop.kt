package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class Laptop(

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val cpuConfigurations: MutableList<LaptopCpu>,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val gpuConfigurations: MutableList<LaptopGpu>,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val ramInfos: MutableList<Ram>,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val storages: MutableList<Storage>,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val laptopScreen: MutableList<Display>,

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
