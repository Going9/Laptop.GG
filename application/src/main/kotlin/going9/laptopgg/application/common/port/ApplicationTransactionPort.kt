package going9.laptopgg.application.common.port

interface ApplicationTransactionPort {
    fun <T> read(block: () -> T): T
    fun <T> write(block: () -> T): T
}
