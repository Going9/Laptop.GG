package going9.laptopgg.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security.password")
data class PasswordHashProperties(
    val bcryptStrength: Int = DEFAULT_BCRYPT_STRENGTH,
) {
    fun normalizedBcryptStrength(): Int {
        return bcryptStrength.coerceIn(MIN_BCRYPT_STRENGTH, MAX_BCRYPT_STRENGTH)
    }

    companion object {
        const val DEFAULT_BCRYPT_STRENGTH = 10
        const val MIN_BCRYPT_STRENGTH = 4
        const val MAX_BCRYPT_STRENGTH = 16
    }
}
