package going9.laptopgg.infrastructure.jpa.transaction

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class JpaTransactionExecutor(
    private val transactionManager: PlatformTransactionManager,
) {
    fun <T> read(block: () -> T): T {
        return execute(readOnly = true, block = block)
    }

    fun <T> write(block: () -> T): T {
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
