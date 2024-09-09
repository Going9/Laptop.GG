package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class Gpu(

    @OneToMany(mappedBy = "gpu")
    val laptops: MutableList<LaptopGpu>,

    val name: String,
    val manufacturer: GpuManufacturer,
    val isIgpu: Boolean,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    ) {

}

enum class GpuManufacturer {
    AMD,
    NVIDIA,
    INTEL,
    QUALCOMM,
    APPLE,
}