package going9.laptopgg.application.comment.port

interface PasswordHashPort {
    fun hash(rawPassword: String): String
    fun matches(rawPassword: String, hashedPassword: String): Boolean
}
