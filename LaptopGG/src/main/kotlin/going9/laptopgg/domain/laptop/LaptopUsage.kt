package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
@Table(name = "laptop_usage")
class LaptopUsage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "laptop_usage")
    val usage: String,

    @ManyToOne
    @JoinColumn(name = "new_laptop_id", nullable = false)
    val newLaptop: NewLaptop
)
