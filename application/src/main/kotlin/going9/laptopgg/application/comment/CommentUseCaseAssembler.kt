package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort

object CommentUseCaseAssembler {
    fun createManageCommentUseCase(
        commentPort: CommentPort,
        laptopPort: CommentLaptopPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): ManageCommentUseCase {
        return DefaultManageCommentUseCase(
            commentPort = commentPort,
            laptopPort = laptopPort,
            passwordHashPort = passwordHashPort,
            transactionPort = transactionPort,
        )
    }
}
