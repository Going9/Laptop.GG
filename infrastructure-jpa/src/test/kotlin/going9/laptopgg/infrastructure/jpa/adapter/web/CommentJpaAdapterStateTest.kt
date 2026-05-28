package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.infrastructure.jpa.repository.web.CommentRepository
import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.web.Comment
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Optional

class CommentJpaAdapterStateTest {
    @Test
    fun `findById rejects persisted comment without generated id with explicit application error`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            laptopRepository = Mockito.mock(WebLaptopRepository::class.java),
        )
        Mockito.`when`(commentRepository.findById(1L)).thenReturn(
            Optional.of(
                Comment(
                    laptop = laptopFixture(),
                    author = "iggy",
                    content = "좋아요",
                    passWord = "hashed:pw",
                ),
            ),
        )

        assertThatThrownBy {
            adapter.findById(1L)
        }.isInstanceOf(ApplicationInvalidStateException::class.java)
    }

    @Test
    fun `findById rejects persisted comment without owning laptop id with explicit application error`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            laptopRepository = Mockito.mock(WebLaptopRepository::class.java),
        )
        Mockito.`when`(commentRepository.findById(1L)).thenReturn(
            Optional.of(
                Comment(
                    laptop = laptopFixture(id = null),
                    author = "iggy",
                    content = "좋아요",
                    passWord = "hashed:pw",
                    id = 1L,
                ),
            ),
        )

        assertThatThrownBy {
            adapter.findById(1L)
        }.isInstanceOf(ApplicationInvalidStateException::class.java)
    }

    private fun laptopFixture(id: Long? = null): Laptop {
        return Laptop(
            name = "Laptop",
            imageUrl = "https://example.com/laptop.jpg",
            detailPage = "https://example.com/laptop",
            price = 1_000_000,
            cpuManufacturer = null,
            cpu = null,
            os = null,
            screenSize = null,
            resolution = null,
            brightness = null,
            refreshRate = null,
            ramSize = null,
            ramType = null,
            isRamReplaceable = null,
            graphicsType = null,
            tgp = null,
            thunderboltCount = null,
            usbCCount = null,
            usbACount = null,
            sdCard = null,
            isSupportsPdCharging = null,
            batteryCapacity = null,
            storageCapacity = null,
            storageSlotCount = null,
            weight = null,
            id = id,
        )
    }
}
