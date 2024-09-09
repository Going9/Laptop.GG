package going9.laptopgg.dto.request

import going9.laptopgg.domain.laptop.Laptop

class CommentRequest(
    val laptopId: Long,
    val author: String,
    val content: String,
    val passWord: String
) {
}