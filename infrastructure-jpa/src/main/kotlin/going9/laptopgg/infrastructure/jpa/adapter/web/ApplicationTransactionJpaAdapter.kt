package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.common.port.ApplicationTransactionPort
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
internal class ApplicationTransactionJpaAdapter(
    private val transactionManager: PlatformTransactionManager,
) : ApplicationTransactionPort {
    override fun <T> read(block: () -> T): T {
        return execute(readOnly = true, block = block)
    }

    override fun <T> write(block: () -> T): T {
        return execute(readOnly = false, block = block)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> execute(readOnly: Boolean, block: () -> T): T {
        val result = TransactionTemplate(transactionManager).apply {
            isReadOnly = readOnly
        }.execute {
            block()
        }
        return result as T
    }
}
