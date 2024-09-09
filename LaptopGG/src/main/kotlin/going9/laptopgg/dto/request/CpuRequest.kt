package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.CpuManufacturer

data class CpuRequest(
    val name: String,
    val manufacturer: CpuManufacturer,
    val isHighPower: Boolean,
) {
    companion object {
        fun default(): CpuRequest {
            return CpuRequest(
                name = "",
                manufacturer = CpuManufacturer.INTEL,
                isHighPower = false,
            )
        }
    }
}