package going9.laptopgg.dto.request

data class StorageRequest(
    val capacity: Int = 0,
    val slot: Int? = null,  // if ssd upgrade possible, input count of ssd slot
) {
}