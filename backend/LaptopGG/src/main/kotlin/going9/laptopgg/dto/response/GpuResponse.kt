package going9.laptopgg.dto.response

import going9.laptopgg.domain.laptop.Gpu
import going9.laptopgg.domain.laptop.GpuManufacturer

class GpuResponse(
    val id: Long,
    val name: String,
    val manufacturer: GpuManufacturer,
    val isIgpu: Boolean,
) {
    companion object {
        fun of(gpu: Gpu): GpuResponse {
            return GpuResponse(
                id = gpu.id!!,
                name = gpu.name,
                manufacturer = gpu.manufacturer,
                isIgpu = gpu.isIgpu,
            )
        }
    }
}