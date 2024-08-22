package going9.laptopgg.dto.request

data class StorageRequest(
    var capacity: Int = 0,
    var slot: Int? = null,  // if ssd upgrade possible, input count of ssd slot
) {
}