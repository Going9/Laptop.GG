package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.common.ApplicationInvalidStateException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.infrastructure.jpa.repository.web.CommentListProjection
import going9.laptopgg.infrastructure.jpa.repository.web.CommentMutationProjection
import going9.laptopgg.infrastructure.jpa.repository.web.CommentRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.web.Comment
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito

class CommentJpaAdapterStateTest {
    @Test
    fun `add saves comment with laptop reference without loading laptop entity`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val entityManager = Mockito.mock(EntityManager::class.java)
        val laptop = laptopFixture(id = 3L)
        val savedCommentCaptor = ArgumentCaptor.forClass(Comment::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = entityManager,
        )
        Mockito.`when`(entityManager.getReference(Laptop::class.java, 3L)).thenReturn(laptop)

        adapter.add(laptopId = 3L, author = "iggy", content = "좋아요", passwordHash = "hashed:pw")

        Mockito.verify(entityManager).getReference(Laptop::class.java, 3L)
        Mockito.verify(commentRepository).save(savedCommentCaptor.capture())
        assertThat(savedCommentCaptor.value.laptop).isSameAs(laptop)
        assertThat(savedCommentCaptor.value.author).isEqualTo("iggy")
        assertThat(savedCommentCaptor.value.content).isEqualTo("좋아요")
        assertThat(savedCommentCaptor.value.passWord).isEqualTo("hashed:pw")
    }

    @Test
    fun `findAllByLaptopId reads comments in persisted id order`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )
        Mockito.`when`(commentRepository.findAllProjectedByLaptop_IdOrderByIdAsc(3L)).thenReturn(
            listOf(
                commentListProjection(id = 1L),
                commentListProjection(id = 2L),
            ),
        )

        val records = adapter.findAllByLaptopId(3L)

        assertThat(records.map { it.id }).containsExactly(1L, 2L)
        Mockito.verify(commentRepository).findAllProjectedByLaptop_IdOrderByIdAsc(3L)
    }

    @Test
    fun `findAllByLaptopId rejects projected comment without generated id with explicit application error`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )
        Mockito.`when`(commentRepository.findAllProjectedByLaptop_IdOrderByIdAsc(3L)).thenReturn(
            listOf(commentListProjection(id = null)),
        )

        assertThatThrownBy {
            adapter.findAllByLaptopId(3L)
        }.isInstanceOf(ApplicationInvalidStateException::class.java)
    }

    @Test
    fun `findMutationById reads only mutation projection`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )

        Mockito.`when`(commentRepository.findMutationProjectedById(1L))
            .thenReturn(commentMutationProjection(id = 1L, laptopId = 3L))

        val record = adapter.findMutationById(1L)

        assertThat(record?.id).isEqualTo(1L)
        assertThat(record?.laptopId).isEqualTo(3L)
        assertThat(record?.passwordHash).isEqualTo("hashed:pw")
        Mockito.verify(commentRepository).findMutationProjectedById(1L)
        Mockito.verify(commentRepository, Mockito.never()).findById(Mockito.anyLong())
    }

    @Test
    fun `findMutationById rejects projected comment without generated id with explicit application error`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )

        Mockito.`when`(commentRepository.findMutationProjectedById(1L))
            .thenReturn(commentMutationProjection(id = null, laptopId = 3L))

        assertThatThrownBy {
            adapter.findMutationById(1L)
        }.isInstanceOf(ApplicationInvalidStateException::class.java)
    }

    @Test
    fun `findMutationById rejects projected comment without owning laptop id with explicit application error`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )

        Mockito.`when`(commentRepository.findMutationProjectedById(1L))
            .thenReturn(commentMutationProjection(id = 1L, laptopId = null))

        assertThatThrownBy {
            adapter.findMutationById(1L)
        }.isInstanceOf(ApplicationInvalidStateException::class.java)
    }

    @Test
    fun `updateContent delegates to direct update query without loading comment entity`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )

        Mockito.`when`(commentRepository.updateContentById(7L, "수정")).thenReturn(1)

        adapter.updateContent(commentId = 7L, content = "수정")

        Mockito.verify(commentRepository).updateContentById(7L, "수정")
        Mockito.verify(commentRepository, Mockito.never()).findById(Mockito.anyLong())
    }

    @Test
    fun `updateContent maps missing direct update row to not found error`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )

        Mockito.`when`(commentRepository.updateContentById(7L, "수정")).thenReturn(0)

        assertThatThrownBy {
            adapter.updateContent(commentId = 7L, content = "수정")
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `deleteById delegates to direct delete query without loading comment entity`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )

        Mockito.`when`(commentRepository.deleteByCommentId(7L)).thenReturn(1)

        adapter.deleteById(7L)

        Mockito.verify(commentRepository).deleteByCommentId(7L)
        Mockito.verify(commentRepository, Mockito.never()).findById(Mockito.anyLong())
    }

    @Test
    fun `deleteById maps missing direct delete row to not found error`() {
        val commentRepository = Mockito.mock(CommentRepository::class.java)
        val adapter = CommentJpaAdapter(
            commentRepository = commentRepository,
            entityManager = Mockito.mock(EntityManager::class.java),
        )

        Mockito.`when`(commentRepository.deleteByCommentId(7L)).thenReturn(0)

        assertThatThrownBy {
            adapter.deleteById(7L)
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    private fun commentListProjection(id: Long?): CommentListProjection {
        return object : CommentListProjection {
            override val id: Long? = id
            override val author: String = "iggy"
            override val content: String = "좋아요"
        }
    }

    private fun commentMutationProjection(id: Long?, laptopId: Long?): CommentMutationProjection {
        return object : CommentMutationProjection {
            override val id: Long? = id
            override val laptopId: Long? = laptopId
            override val passwordHash: String = "hashed:pw"
        }
    }

    private fun laptopFixture(id: Long): Laptop {
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
