package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.Laptop

data class CommentRequest(
    val laptopId: Long = 0,
    val author: String = "",
    val content: String = "",
    val passWord: String = ""
) {
}