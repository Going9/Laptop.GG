package going9.laptopgg.dto.request

data class CommentRequest(
    val laptopId: Long = 0,
    val author: String = "",
    val content: String = "",
    val passWord: String = ""
) {
}