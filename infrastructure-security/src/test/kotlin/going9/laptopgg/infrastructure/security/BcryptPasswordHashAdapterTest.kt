package going9.laptopgg.infrastructure.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BcryptPasswordHashAdapterTest {
    @Test
    fun `72 UTF8 byte password hashes and authenticates without truncation`() {
        val adapter = BcryptPasswordHashAdapter(PasswordHashProperties(bcryptStrength = PasswordHashProperties.MIN_BCRYPT_STRENGTH))
        val password = "가".repeat(12) + java.util.UUID.randomUUID().toString()
        val hash = adapter.hash(password)
        assertThat(adapter.matches(password, hash)).isTrue()
        assertThat(adapter.matches(password.dropLast(1) + "!", hash)).isFalse()
    }

    @Test
    fun `hashes and verifies password with configured bcrypt strength`() {
        val adapter = BcryptPasswordHashAdapter(
            PasswordHashProperties(bcryptStrength = PasswordHashProperties.MIN_BCRYPT_STRENGTH),
        )

        val hash = adapter.hash("secret")

        assertThat(adapter.matches("secret", hash)).isTrue()
        assertThat(adapter.matches("wrong", hash)).isFalse()
    }

    @Test
    fun `bcrypt strength is bounded to operational range`() {
        assertThat(PasswordHashProperties(bcryptStrength = 1).normalizedBcryptStrength())
            .isEqualTo(PasswordHashProperties.MIN_BCRYPT_STRENGTH)
        assertThat(PasswordHashProperties(bcryptStrength = 31).normalizedBcryptStrength())
            .isEqualTo(PasswordHashProperties.MAX_BCRYPT_STRENGTH)
    }
}
