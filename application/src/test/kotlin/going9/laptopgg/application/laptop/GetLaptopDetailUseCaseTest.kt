package going9.laptopgg.application.laptop

import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.port.LaptopDetailRecord
import going9.laptopgg.application.laptop.port.LaptopPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class GetLaptopDetailUseCaseTest {
    @Test
    fun `detail query rejects invalid laptop id before persistence`() {
        val laptopPort = RecordingLaptopPort()
        val transactionPort = RecordingApplicationTransactionPort()
        val useCase = LaptopUseCaseAssembler.createGetLaptopDetailUseCase(
            laptopPort = laptopPort,
            transactionPort = transactionPort,
        )

        assertThatThrownBy {
            useCase.get(0L)
        }.isInstanceOf(InvalidCommandException::class.java)

        assertThat(laptopPort.findDetailCalls).isZero()
        assertThat(transactionPort.readCalls).isZero()
    }

    private class RecordingLaptopPort : LaptopPort {
        var findDetailCalls = 0
            private set

        override fun findDetailById(laptopId: Long): LaptopDetailRecord? {
            findDetailCalls++
            return null
        }
    }

    private class RecordingApplicationTransactionPort : ApplicationTransactionPort {
        var readCalls = 0
            private set
        var writeCalls = 0
            private set

        override fun <T> read(block: () -> T): T {
            readCalls++
            return block()
        }

        override fun <T> write(block: () -> T): T {
            writeCalls++
            return block()
        }
    }
}
