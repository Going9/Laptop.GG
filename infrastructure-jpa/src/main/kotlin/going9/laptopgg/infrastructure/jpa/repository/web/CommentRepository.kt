package going9.laptopgg.infrastructure.jpa.repository.web

import going9.laptopgg.persistence.model.web.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<Comment, Long> {
    fun findAllProjectedByLaptop_IdOrderByIdAsc(laptopId: Long): List<CommentListProjection>

    @Query(
        """
        select c.id as id,
               c.laptop.id as laptopId,
               c.passWord as passwordHash
        from Comment c
        where c.id = :commentId
        """,
    )
    fun findMutationProjectedById(@Param("commentId") commentId: Long): CommentMutationProjection?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.content = :content where c.id = :commentId")
    fun updateContentById(
        @Param("commentId") commentId: Long,
        @Param("content") content: String,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.id = :commentId")
    fun deleteByCommentId(@Param("commentId") commentId: Long): Int
}

interface CommentListProjection {
    val id: Long?
    val author: String
    val content: String
}

interface CommentMutationProjection {
    val id: Long?
    val laptopId: Long?
    val passwordHash: String
}
