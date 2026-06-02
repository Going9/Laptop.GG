package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.comment.port.CommentMutationRecord
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.AuthenticationFailedException
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort

internal class CommentMutationGuard(
    private val commentMutationPort: CommentMutationPort,
    private val passwordHashPort: PasswordHashPort,
    private val transactionPort: ApplicationTransactionPort,
) {
    fun findAuthorizedComment(commentId: Long, password: String): CommentMutationRecord {
        val comment = transactionPort.read {
            commentMutationPort.findMutationById(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
        }
        if (!passwordHashPort.matches(password, comment.passwordHash)) {
            throw AuthenticationFailedException("비밀번호가 일치하지 않습니다.")
        }
        return comment
    }
}
