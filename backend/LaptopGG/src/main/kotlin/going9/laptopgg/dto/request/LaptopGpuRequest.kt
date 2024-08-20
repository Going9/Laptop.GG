package going9.laptopgg.dto.request

class LaptopGpuRequest(
    val gpuId: Long,
    val tgp: Int? = null,
    val isMux: Boolean,
) {
}