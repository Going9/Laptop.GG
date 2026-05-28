package going9.laptopgg.persistence.model.web

import going9.laptopgg.persistence.model.laptop.Laptop
import jakarta.persistence.*

@Entity
class Comment(
    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    var author: String,
    var content: String,
    val passWord: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
    fun updateComment(content: String) {
        this.content = content
    }
}
