package going9.laptopgg.infrastructure.security

import going9.laptopgg.application.comment.port.PasswordHashPort
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PasswordHashProperties::class)
class PasswordHashAdapterConfig {
    @Bean
    fun passwordHashPort(properties: PasswordHashProperties): PasswordHashPort {
        return BcryptPasswordHashAdapter(properties)
    }
}
