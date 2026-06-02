package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.infrastructure.jpa.transaction.JpaTransactionExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager

@Component
internal class ApplicationTransactionJpaAdapter(
    transactionManager: PlatformTransactionManager,
) : ApplicationTransactionPort {
    private val transactionExecutor = JpaTransactionExecutor(transactionManager)

    override fun <T> read(block: () -> T): T {
        return transactionExecutor.read(block)
    }

    override fun <T> write(block: () -> T): T {
        return transactionExecutor.write(block)
    }
}
