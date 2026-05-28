package going9.laptopgg.infrastructure.security

import going9.laptopgg.application.comment.port.PasswordHashPort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordHashAdapter(
    properties: PasswordHashProperties,
) : PasswordHashPort {
    private val encoder = BCryptPasswordEncoder(properties.normalizedBcryptStrength())

    override fun hash(rawPassword: String): String {
        return encoder.encode(rawPassword)
    }

    override fun matches(rawPassword: String, hashedPassword: String): Boolean {
        return encoder.matches(rawPassword, hashedPassword)
    }
}
