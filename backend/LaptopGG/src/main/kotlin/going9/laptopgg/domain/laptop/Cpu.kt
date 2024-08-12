package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class Cpu(

    @OneToMany(mappedBy = "cpu")
    val laptopCpus: MutableList<LaptopCpu>,

    val name: String,
    val isHighPower: Boolean,
    val manufacturer: CpuManufacturer,

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