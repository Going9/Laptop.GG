package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class Laptop(

    val imgLink: String,
    val price: Int,
    val priceLink: String,
    val name: String,
    val manufacturer: String,
    val mainCategory: String,
    val subCategory: String,
    val weight: Int,
    val thunderBoltPorts: Int? = null,
    val usb4Ports: Int? = null,
    val batteryCapacity: Int,
    val sdCardType: String,

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val cpus: MutableList<LaptopCpu> = mutableListOf(),

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val gpus: MutableList<LaptopGpu> = mutableListOf(),

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val rams: MutableList<Ram> = mutableListOf(),

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val storages: MutableList<Storage> = mutableListOf(),

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    val displays: MutableList<Display> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
