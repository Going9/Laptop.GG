package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class Cpu(

    @OneToMany(mappedBy = "cpu")
    val laptopCpus: MutableList<LaptopCpu>,

    val name: String,
    val manufacturer: CpuManufacturer,
    val isHighPower: Boolean,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    ) {
}

enum class CpuManufacturer {
    INTEL,
    AMD,
    APPLE,
    QUALCOMM
}