package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.GpuManufacturer

class GpuRequest(
    val name: String,
    val manufacturer: GpuManufacturer,
    val isIGpu: Boolean
)