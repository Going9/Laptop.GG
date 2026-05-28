package going9.laptopgg.application.comment

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.application.comment.port.CommentMutationPort
import going9.laptopgg.application.comment.port.CommentQueryPort
import going9.laptopgg.application.comment.port.PasswordHashPort
import going9.laptopgg.application.common.port.ApplicationTransactionPort

object CommentUseCaseAssembler {
    fun createAddCommentUseCase(
        commentMutationPort: CommentMutationPort,
        laptopPort: CommentLaptopPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): AddCommentUseCase {
        return DefaultAddCommentUseCase(
            commentMutationPort = commentMutationPort,
            laptopPort = laptopPort,
            passwordHashPort = passwordHashPort,
            transactionPort = transactionPort,
        )
    }

    fun createListLaptopCommentsUseCase(
        commentQueryPort: CommentQueryPort,
        laptopPort: CommentLaptopPort,
        transactionPort: ApplicationTransactionPort,
    ): ListLaptopCommentsUseCase {
        return DefaultListLaptopCommentsUseCase(
            commentQueryPort = commentQueryPort,
            laptopPort = laptopPort,
            transactionPort = transactionPort,
        )
    }

    fun createUpdateCommentUseCase(
        commentMutationPort: CommentMutationPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): UpdateCommentUseCase {
        return DefaultUpdateCommentUseCase(
            commentMutationPort = commentMutationPort,
            mutationGuard = createMutationGuard(
                commentMutationPort = commentMutationPort,
                passwordHashPort = passwordHashPort,
                transactionPort = transactionPort,
            ),
            transactionPort = transactionPort,
        )
    }

    fun createDeleteCommentUseCase(
        commentMutationPort: CommentMutationPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): DeleteCommentUseCase {
        return DefaultDeleteCommentUseCase(
            commentMutationPort = commentMutationPort,
            mutationGuard = createMutationGuard(
                commentMutationPort = commentMutationPort,
                passwordHashPort = passwordHashPort,
                transactionPort = transactionPort,
            ),
            transactionPort = transactionPort,
        )
    }

    private fun createMutationGuard(
        commentMutationPort: CommentMutationPort,
        passwordHashPort: PasswordHashPort,
        transactionPort: ApplicationTransactionPort,
    ): CommentMutationGuard {
        return CommentMutationGuard(
            commentMutationPort = commentMutationPort,
            passwordHashPort = passwordHashPort,
            transactionPort = transactionPort,
        )
    }
}
