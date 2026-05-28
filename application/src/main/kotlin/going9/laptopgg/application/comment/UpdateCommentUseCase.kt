package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort

interface UpdateCommentUseCase {
    fun update(commentId: Long, command: UpdateCommentCommand): CommentMutationResult
}

internal class DefaultUpdateCommentUseCase(
    private val commentMutationPort: CommentMutationPort,
    private val mutationGuard: CommentMutationGuard,
    private val transactionPort: ApplicationTransactionPort,
) : UpdateCommentUseCase {
    override fun update(commentId: Long, command: UpdateCommentCommand): CommentMutationResult {
        CommentCommandValidator.validateCommentId(commentId)
        CommentCommandValidator.validateUpdate(command)
        val content = CommentCommandValidator.normalizeDisplayText(command.content)
        val comment = mutationGuard.findAuthorizedComment(commentId, command.password)
        return transactionPort.write {
            commentMutationPort.updateContent(commentId, content)
            CommentMutationResult(laptopId = comment.laptopId)
        }
    }
}
