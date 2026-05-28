package going9.laptopgg.infrastructure.security

import going9.laptopgg.application.port.out.PasswordHashPort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordHashAdapter : PasswordHashPort {
    private val encoder = BCryptPasswordEncoder(4)

    override fun hash(rawPassword: String): String {
        return encoder.encode(rawPassword)
    }

    override fun matches(rawPassword: String, hashedPassword: String): Boolean {
        return encoder.matches(rawPassword, hashedPassword)
    }
}
