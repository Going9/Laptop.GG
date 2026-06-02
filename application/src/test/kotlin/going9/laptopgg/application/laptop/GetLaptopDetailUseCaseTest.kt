package going9.laptopgg.application.laptop

import going9.laptopgg.application.comment.port.CommentListRecord
import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
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

    @Test
    fun `detail page query reads laptop and comments in one read transaction`() {
        val laptopPort = RecordingLaptopPort(laptop = laptopDetailRecord(id = 3L))
        val commentPort = RecordingCommentPort(
            records = listOf(
                CommentListRecord(
                    id = 7L,
                    author = "iggy",
                    content = "좋아요",
                ),
            ),
        )
        val transactionPort = RecordingApplicationTransactionPort()
        val useCase = LaptopUseCaseAssembler.createGetLaptopDetailPageUseCase(
            laptopPort = laptopPort,
            commentQueryPort = commentPort,
            transactionPort = transactionPort,
        )

        val result = useCase.get(3L)

        assertThat(result.laptopDetail.id).isEqualTo(3L)
        assertThat(result.comments.map { it.content }).containsExactly("좋아요")
        assertThat(laptopPort.findDetailCalls).isEqualTo(1)
        assertThat(commentPort.findAllByLaptopCalls).isEqualTo(1)
        assertThat(transactionPort.readCalls).isEqualTo(1)
        assertThat(transactionPort.writeCalls).isZero()
    }

    @Test
    fun `detail page query does not read comments when laptop is missing`() {
        val laptopPort = RecordingLaptopPort()
        val commentPort = RecordingCommentPort()
        val transactionPort = RecordingApplicationTransactionPort()
        val useCase = LaptopUseCaseAssembler.createGetLaptopDetailPageUseCase(
            laptopPort = laptopPort,
            commentQueryPort = commentPort,
            transactionPort = transactionPort,
        )

        assertThatThrownBy {
            useCase.get(404L)
        }.isInstanceOf(ResourceNotFoundException::class.java)

        assertThat(laptopPort.findDetailCalls).isEqualTo(1)
        assertThat(commentPort.findAllByLaptopCalls).isZero()
        assertThat(transactionPort.readCalls).isEqualTo(1)
    }

    @Test
    fun `detail page query rejects invalid laptop id before persistence`() {
        val laptopPort = RecordingLaptopPort()
        val commentPort = RecordingCommentPort()
        val transactionPort = RecordingApplicationTransactionPort()
        val useCase = LaptopUseCaseAssembler.createGetLaptopDetailPageUseCase(
            laptopPort = laptopPort,
            commentQueryPort = commentPort,
            transactionPort = transactionPort,
        )

        assertThatThrownBy {
            useCase.get(0L)
        }.isInstanceOf(InvalidCommandException::class.java)

        assertThat(laptopPort.findDetailCalls).isZero()
        assertThat(commentPort.findAllByLaptopCalls).isZero()
        assertThat(transactionPort.readCalls).isZero()
    }

    private class RecordingLaptopPort(
        private val laptop: LaptopDetailRecord? = null,
    ) : LaptopPort {
        var findDetailCalls = 0
            private set

        override fun findDetailById(laptopId: Long): LaptopDetailRecord? {
            findDetailCalls++
            return laptop
        }
    }

    private class RecordingCommentPort(
        private val records: List<CommentListRecord> = emptyList(),
    ) : CommentQueryPort {
        var findAllByLaptopCalls = 0
            private set

        override fun findAllByLaptopId(laptopId: Long): List<CommentListRecord> {
            findAllByLaptopCalls++
            return records
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

    private fun laptopDetailRecord(id: Long): LaptopDetailRecord {
        return LaptopDetailRecord(
            id = id,
            name = "테스트 노트북",
            imageUrl = "https://example.com/laptop.jpg",
            detailPage = "https://prod.danawa.com/info/?pcode=1&cate=112758",
            price = 1_490_000,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra",
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "1920x1200",
            brightness = 300,
            refreshRate = 60,
            ramSize = 16,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = "Intel Graphics",
            tgp = 0,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 60.0,
            storageCapacity = 512,
            storageSlotCount = 1,
            weight = 1.2,
            usage = listOf("사무/인강용"),
        )
    }
}
