package going9.laptopgg.application.port.out

interface ApplicationTransactionPort {
    fun <T> read(block: () -> T): T
    fun <T> write(block: () -> T): T
}
