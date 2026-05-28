package going9.laptopgg.application.laptop

import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.port.LaptopPort

interface GetLaptopDetailUseCase {
    fun get(laptopId: Long): LaptopDetailResult
}

internal class DefaultGetLaptopDetailUseCase(
    private val laptopPort: LaptopPort,
    private val transactionPort: ApplicationTransactionPort,
) : GetLaptopDetailUseCase {
    override fun get(laptopId: Long): LaptopDetailResult {
        validateLaptopId(laptopId)
        return transactionPort.read {
            val laptop = laptopPort.findDetailById(laptopId) ?: throw ResourceNotFoundException("Laptop", laptopId)
            laptop.toLaptopDetailResult()
        }
    }

    private fun validateLaptopId(laptopId: Long) {
        if (laptopId <= 0) {
            throw InvalidCommandException("laptopId must be positive.")
        }
    }
}
