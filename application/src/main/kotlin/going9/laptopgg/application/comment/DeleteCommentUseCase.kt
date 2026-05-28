package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort

interface DeleteCommentUseCase {
    fun delete(commentId: Long, command: DeleteCommentCommand): CommentMutationResult
}

internal class DefaultDeleteCommentUseCase(
    private val commentMutationPort: CommentMutationPort,
    private val mutationGuard: CommentMutationGuard,
    private val transactionPort: ApplicationTransactionPort,
) : DeleteCommentUseCase {
    override fun delete(commentId: Long, command: DeleteCommentCommand): CommentMutationResult {
        CommentCommandValidator.validateCommentId(commentId)
        CommentCommandValidator.validateDelete(command)
        val comment = mutationGuard.findAuthorizedComment(commentId, command.password)
        return transactionPort.write {
            commentMutationPort.deleteById(commentId)
            CommentMutationResult(laptopId = comment.laptopId)
        }
    }
}
