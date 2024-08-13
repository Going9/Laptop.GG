package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.GpuManufacturer

class GpuRequest(
    val name: String,
    val manufacturer: GpuManufacturer,
    val isIgpu: Boolean,
) {
    companion object {
        fun default(): GpuRequest {
            return GpuRequest(
                name = "",
                manufacturer = GpuManufacturer.NVIDIA,
                isIgpu = false,
            )
        }
    }
}