package going9.laptopgg.dto.response

import going9.laptopgg.domain.laptop.Cpu
import going9.laptopgg.domain.laptop.CpuManufacturer

class CpuResponse(
    val name: String,
    val isHighPower: Boolean,
    val manufacturer: CpuManufacturer,
) {
    companion object {
        fun of(cpu: Cpu): CpuResponse {
            return CpuResponse(
                name = cpu.name,
                isHighPower = cpu.isHighPower,
                manufacturer = cpu.manufacturer,
            )
        }
    }
}