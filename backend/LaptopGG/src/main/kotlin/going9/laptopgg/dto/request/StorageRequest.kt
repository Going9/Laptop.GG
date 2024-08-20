package going9.laptopgg.dto.request

class StorageRequest(
    val capacity: Int,
    val slot: Int? = null,  // if ssd upgrade possible, input count of ssd slot
) {
}