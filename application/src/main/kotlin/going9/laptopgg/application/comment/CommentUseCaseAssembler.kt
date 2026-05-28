package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort

object CommentUseCaseAssembler {
    fun createManageCommentUseCase(
        commentQueryPort: CommentQueryPort,
        commentMutationPort: CommentMutationPort,
        laptopPort: CommentLaptopPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): ManageCommentUseCase {
        return DefaultManageCommentUseCase(
            commentQueryPort = commentQueryPort,
            commentMutationPort = commentMutationPort,
            laptopPort = laptopPort,
            passwordHashPort = passwordHashPort,
            transactionPort = transactionPort,
        )
    }
}
