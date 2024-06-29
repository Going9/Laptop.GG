package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.CpuManufacturer

class CpuRequest(
    val name: String,
    val isHighPower: Boolean,
    val manufacturer: CpuManufacturer
)