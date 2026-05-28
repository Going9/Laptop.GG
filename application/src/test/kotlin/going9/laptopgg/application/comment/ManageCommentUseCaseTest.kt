package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.comment.port.CommentRecord
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.AuthenticationFailedException
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ManageCommentUseCaseTest {
    private val commentPort = InMemoryCommentPort()
    private val laptopPort = InMemoryCommentLaptopPort(existingIds = setOf(1L))
    private val passwordHashPort = PlainPasswordHashPort()
    private val useCase = CommentUseCaseAssembler.createManageCommentUseCase(
        commentPort = commentPort,
        laptopPort = laptopPort,
        passwordHashPort = passwordHashPort,
        transactionPort = DirectApplicationTransactionPort,
    )

    @Test
    fun `add rejects blank comment fields before persistence`() {
        assertThatThrownBy {
            useCase.add(AddCommentCommand(laptopId = 1L, author = "", content = "좋아요", password = "pw"))
        }.isInstanceOf(InvalidCommandException::class.java)

        assertThat(commentPort.records).isEmpty()
    }

    @Test
    fun `add rejects missing laptop with explicit not found error`() {
        assertThatThrownBy {
            useCase.add(AddCommentCommand(laptopId = 99L, author = "iggy", content = "좋아요", password = "pw"))
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `update rejects password mismatch with explicit authentication error`() {
        commentPort.records[7L] = CommentRecord(
            id = 7L,
            author = "iggy",
            content = "좋아요",
            passwordHash = "hashed:secret",
        )

        assertThatThrownBy {
            useCase.update(7L, UpdateCommentCommand(password = "wrong", content = "수정"))
        }.isInstanceOf(AuthenticationFailedException::class.java)
    }

    private class InMemoryCommentPort : CommentPort {
        val records = mutableMapOf<Long, CommentRecord>()
        private var nextId = 1L

        override fun findById(commentId: Long): CommentRecord? {
            return records[commentId]
        }

        override fun findAllByLaptopId(laptopId: Long): List<CommentRecord> {
            return records.values.toList()
        }

        override fun add(laptopId: Long, author: String, content: String, passwordHash: String) {
            val id = nextId++
            records[id] = CommentRecord(id = id, author = author, content = content, passwordHash = passwordHash)
        }

        override fun updateContent(commentId: Long, content: String) {
            val current = records.getValue(commentId)
            records[commentId] = current.copy(content = content)
        }

        override fun deleteById(commentId: Long) {
            records.remove(commentId)
        }
    }

    private class InMemoryCommentLaptopPort(
        private val existingIds: Set<Long>,
    ) : CommentLaptopPort {
        override fun existsById(laptopId: Long): Boolean {
            return laptopId in existingIds
        }
    }

    private class PlainPasswordHashPort : PasswordHashPort {
        override fun hash(rawPassword: String): String {
            return "hashed:$rawPassword"
        }

        override fun matches(rawPassword: String, hashedPassword: String): Boolean {
            return hash(rawPassword) == hashedPassword
        }
    }

    private object DirectApplicationTransactionPort : ApplicationTransactionPort {
        override fun <T> read(block: () -> T): T {
            return block()
        }

        override fun <T> write(block: () -> T): T {
            return block()
        }
    }
}
