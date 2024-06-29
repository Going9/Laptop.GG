package going9.laptopgg.dto.request

class LaptopRequest(
    val weight: Int,
    val thunderVolt: Int? = null,
    val usb4: Int? = null,
    val battery: Int,
    val name: String
)
