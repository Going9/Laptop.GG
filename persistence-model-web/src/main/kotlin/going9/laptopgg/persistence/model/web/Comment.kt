package going9.laptopgg.persistence.model.web

import going9.laptopgg.persistence.model.laptop.Laptop
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Comment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laptop_id", nullable = false)
    val laptop: Laptop,

    @Column(nullable = false)
    var author: String,

    @Column(nullable = false)
    var content: String,

    @Column(name = "pass_word", nullable = false)
    val passWord: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    fun updateComment(content: String) {
        this.content = content
    }
}
