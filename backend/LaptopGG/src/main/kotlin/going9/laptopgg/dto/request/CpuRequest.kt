package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.CpuManufacturer

class CpuRequest(
    val name: String? = null,
    val isHighPower: Boolean? = null,
    val manufacturer: CpuManufacturer? =null
)