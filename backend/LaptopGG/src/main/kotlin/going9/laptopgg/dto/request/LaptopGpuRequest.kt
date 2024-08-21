package going9.laptopgg.dto.request

data class LaptopGpuRequest(
    var gpuId: Long = 0,
    var tgp: Int? = null,
    var isMux: Boolean = false,
) {
}