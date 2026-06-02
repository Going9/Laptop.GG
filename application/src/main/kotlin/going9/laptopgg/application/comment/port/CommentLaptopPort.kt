package going9.laptopgg.application.comment.port

interface CommentLaptopPort {
    fun existsById(laptopId: Long): Boolean
}
