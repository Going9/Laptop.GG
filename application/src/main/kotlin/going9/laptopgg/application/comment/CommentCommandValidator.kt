package going9.laptopgg.application.comment

import going9.laptopgg.application.common.InvalidCommandException

internal object CommentCommandValidator {
    fun validateAdd(command: AddCommentCommand) {
        requirePositiveId(fieldName = "laptopId", value = command.laptopId)
        requireNonBlank(fieldName = "author", value = command.author)
        requireContent(command.content)
        requireNonBlank(fieldName = "password", value = command.password)
    }

    fun validateLaptopId(laptopId: Long) {
        requirePositiveId(fieldName = "laptopId", value = laptopId)
    }

    fun validateCommentId(commentId: Long) {
        requirePositiveId(fieldName = "commentId", value = commentId)
    }

    fun validateUpdate(command: UpdateCommentCommand) {
        requireContent(command.content)
        requireNonBlank(fieldName = "password", value = command.password)
    }

    fun validateDelete(command: DeleteCommentCommand) {
        requireNonBlank(fieldName = "password", value = command.password)
    }

    fun normalizeDisplayText(value: String): String {
        return value.trim()
    }

    private fun requireContent(value: String) {
        requireNonBlank(fieldName = "content", value = value)
        if (value.length > 255) {
            throw InvalidCommandException("content must be at most 255 characters.")
        }
    }

    private fun requirePositiveId(fieldName: String, value: Long) {
        if (value <= 0) {
            throw InvalidCommandException("$fieldName must be positive.")
        }
    }

    private fun requireNonBlank(fieldName: String, value: String) {
        if (value.isBlank()) {
            throw InvalidCommandException("$fieldName must not be blank.")
        }
    }
}
