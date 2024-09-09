package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class Comment(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    val author: String,
    val content: String,
    val passWord: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {
}