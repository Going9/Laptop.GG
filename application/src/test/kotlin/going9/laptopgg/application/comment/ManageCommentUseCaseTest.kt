package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentListRecord
import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.comment.port.CommentMutationRecord
import going9.laptopgg.application.comment.port.CommentQueryPort
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
    private val transactionPort = RecordingApplicationTransactionPort()
    private val passwordHashPort = PlainPasswordHashPort(transactionPort)
    private val useCase = CommentUseCaseAssembler.createManageCommentUseCase(
        commentQueryPort = commentPort,
        commentMutationPort = commentPort,
        laptopPort = laptopPort,
        passwordHashPort = passwordHashPort,
        transactionPort = transactionPort,
    )

    @Test
    fun `add rejects blank comment fields before persistence`() {
        assertThatThrownBy {
            useCase.add(AddCommentCommand(laptopId = 1L, author = "", content = "좋아요", password = "pw"))
        }.isInstanceOf(InvalidCommandException::class.java)

        assertThat(commentPort.records).isEmpty()
        assertThat(transactionPort.writeCalls).isZero()
    }

    @Test
    fun `add hashes password outside transaction and keeps database work scoped`() {
        useCase.add(AddCommentCommand(laptopId = 1L, author = "iggy", content = "좋아요", password = "pw"))

        assertThat(commentPort.records.values.single().passwordHash).isEqualTo("hashed:pw")
        assertThat(transactionPort.readCalls).isEqualTo(1)
        assertThat(transactionPort.writeCalls).isEqualTo(1)
        assertThat(passwordHashPort.hashCalls).isEqualTo(1)
        assertThat(passwordHashPort.hashInsideTransaction).isFalse()
    }

    @Test
    fun `add normalizes display text at application boundary while preserving raw password`() {
        useCase.add(AddCommentCommand(laptopId = 1L, author = "  iggy  ", content = "  좋아요  ", password = " pw "))

        val savedComment = commentPort.records.values.single()
        assertThat(savedComment.author).isEqualTo("iggy")
        assertThat(savedComment.content).isEqualTo("좋아요")
        assertThat(savedComment.passwordHash).isEqualTo("hashed: pw ")
    }

    @Test
    fun `add rejects missing laptop with explicit not found error`() {
        assertThatThrownBy {
            useCase.add(AddCommentCommand(laptopId = 99L, author = "iggy", content = "좋아요", password = "pw"))
        }.isInstanceOf(ResourceNotFoundException::class.java)

        assertThat(passwordHashPort.hashCalls).isZero()
        assertThat(transactionPort.writeCalls).isZero()
    }

    @Test
    fun `list rejects missing laptop before reading comments`() {
        assertThatThrownBy {
            useCase.listByLaptop(99L)
        }.isInstanceOf(ResourceNotFoundException::class.java)

        assertThat(commentPort.findAllByLaptopCalls).isZero()
    }

    @Test
    fun `list rejects invalid laptop id before reading comments`() {
        assertThatThrownBy {
            useCase.listByLaptop(0L)
        }.isInstanceOf(InvalidCommandException::class.java)

        assertThat(commentPort.findAllByLaptopCalls).isZero()
        assertThat(transactionPort.readCalls).isZero()
    }

    @Test
    fun `update rejects password mismatch with explicit authentication error`() {
        commentPort.records[7L] = StoredComment(
            id = 7L,
            laptopId = 1L,
            author = "iggy",
            content = "좋아요",
            passwordHash = "hashed:secret",
        )

        assertThatThrownBy {
            useCase.update(7L, UpdateCommentCommand(password = "wrong", content = "수정"))
        }.isInstanceOf(AuthenticationFailedException::class.java)

        assertThat(transactionPort.readCalls).isEqualTo(1)
        assertThat(transactionPort.writeCalls).isZero()
        assertThat(passwordHashPort.matchesInsideTransaction).isFalse()
    }

    @Test
    fun `update returns owning laptop id from persisted comment`() {
        commentPort.records[7L] = StoredComment(
            id = 7L,
            laptopId = 3L,
            author = "iggy",
            content = "좋아요",
            passwordHash = "hashed:secret",
        )

        val result = useCase.update(7L, UpdateCommentCommand(password = "secret", content = "수정"))

        assertThat(result.laptopId).isEqualTo(3L)
        assertThat(commentPort.records.getValue(7L).content).isEqualTo("수정")
        assertThat(transactionPort.readCalls).isEqualTo(1)
        assertThat(transactionPort.writeCalls).isEqualTo(1)
        assertThat(passwordHashPort.matchesInsideTransaction).isFalse()
    }

    @Test
    fun `update normalizes display content at application boundary`() {
        commentPort.records[7L] = StoredComment(
            id = 7L,
            laptopId = 3L,
            author = "iggy",
            content = "좋아요",
            passwordHash = "hashed:secret",
        )

        useCase.update(7L, UpdateCommentCommand(password = "secret", content = "  수정  "))

        assertThat(commentPort.records.getValue(7L).content).isEqualTo("수정")
    }

    @Test
    fun `delete returns owning laptop id from persisted comment`() {
        commentPort.records[7L] = StoredComment(
            id = 7L,
            laptopId = 3L,
            author = "iggy",
            content = "좋아요",
            passwordHash = "hashed:secret",
        )

        val result = useCase.delete(7L, DeleteCommentCommand(password = "secret"))

        assertThat(result.laptopId).isEqualTo(3L)
        assertThat(commentPort.records).doesNotContainKey(7L)
        assertThat(transactionPort.readCalls).isEqualTo(1)
        assertThat(transactionPort.writeCalls).isEqualTo(1)
        assertThat(passwordHashPort.matchesInsideTransaction).isFalse()
    }

    @Test
    fun `update rejects invalid comment id before reading comment`() {
        assertThatThrownBy {
            useCase.update(0L, UpdateCommentCommand(password = "pw", content = "수정"))
        }.isInstanceOf(InvalidCommandException::class.java)

        assertThat(commentPort.findMutationByIdCalls).isZero()
        assertThat(transactionPort.writeCalls).isZero()
    }

    @Test
    fun `delete rejects invalid comment id before reading comment`() {
        assertThatThrownBy {
            useCase.delete(0L, DeleteCommentCommand(password = "pw"))
        }.isInstanceOf(InvalidCommandException::class.java)

        assertThat(commentPort.findMutationByIdCalls).isZero()
        assertThat(transactionPort.writeCalls).isZero()
    }

    private class InMemoryCommentPort : CommentQueryPort, CommentMutationPort {
        val records = mutableMapOf<Long, StoredComment>()
        var findAllByLaptopCalls = 0
            private set
        var findMutationByIdCalls = 0
            private set
        private var nextId = 1L

        override fun findMutationById(commentId: Long): CommentMutationRecord? {
            findMutationByIdCalls++
            return records[commentId]?.let { record ->
                CommentMutationRecord(
                    id = record.id,
                    laptopId = record.laptopId,
                    passwordHash = record.passwordHash,
                )
            }
        }

        override fun findAllByLaptopId(laptopId: Long): List<CommentListRecord> {
            findAllByLaptopCalls++
            return records.values.map { record ->
                CommentListRecord(
                    id = record.id,
                    author = record.author,
                    content = record.content,
                )
            }
        }

        override fun add(laptopId: Long, author: String, content: String, passwordHash: String) {
            val id = nextId++
            records[id] = StoredComment(
                id = id,
                laptopId = laptopId,
                author = author,
                content = content,
                passwordHash = passwordHash,
            )
        }

        override fun updateContent(commentId: Long, content: String) {
            val current = records.getValue(commentId)
            records[commentId] = current.copy(content = content)
        }

        override fun deleteById(commentId: Long) {
            records.remove(commentId)
        }
    }

    private data class StoredComment(
        val id: Long,
        val laptopId: Long,
        val author: String,
        val content: String,
        val passwordHash: String,
    )

    private class InMemoryCommentLaptopPort(
        private val existingIds: Set<Long>,
    ) : CommentLaptopPort {
        override fun existsById(laptopId: Long): Boolean {
            return laptopId in existingIds
        }
    }

    private class PlainPasswordHashPort(
        private val transactionPort: RecordingApplicationTransactionPort,
    ) : PasswordHashPort {
        var hashCalls = 0
            private set
        var matchesCalls = 0
            private set
        var hashInsideTransaction = false
            private set
        var matchesInsideTransaction = false
            private set

        override fun hash(rawPassword: String): String {
            hashCalls++
            hashInsideTransaction = hashInsideTransaction || transactionPort.insideTransaction
            return "hashed:$rawPassword"
        }

        override fun matches(rawPassword: String, hashedPassword: String): Boolean {
            matchesCalls++
            matchesInsideTransaction = matchesInsideTransaction || transactionPort.insideTransaction
            return "hashed:$rawPassword" == hashedPassword
        }
    }

    private class RecordingApplicationTransactionPort : ApplicationTransactionPort {
        var readCalls = 0
            private set
        var writeCalls = 0
            private set
        var insideTransaction = false
            private set

        override fun <T> read(block: () -> T): T {
            readCalls++
            return enter(block)
        }

        override fun <T> write(block: () -> T): T {
            writeCalls++
            return enter(block)
        }

        private fun <T> enter(block: () -> T): T {
            check(!insideTransaction) { "Nested application transaction was called." }
            insideTransaction = true
            return try {
                block()
            } finally {
                insideTransaction = false
            }
        }
    }
}
