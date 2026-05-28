package going9.laptopgg.job.crawler.danawa.client

import going9.laptopgg.job.crawler.danawa.detail.DetailRequestContext
import going9.laptopgg.job.crawler.list.ProductCard
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DanawaRequestFactoryTest {
    private val requestFactory = DanawaRequestFactory()

    @Test
    fun `detail spec request is skipped when request context is incomplete`() {
        val productCard = productCard()

        assertThat(requestFactory.detailSpec(productCard, null)).isNull()
        assertThat(
            requestFactory.detailSpec(
                productCard,
                DetailRequestContext(makerName = "레노버", productName = "", prodType = "32741"),
            ),
        ).isNull()
    }

    @Test
    fun `detail spec request keeps referer and ajax endpoint`() {
        val request = requestFactory.detailSpec(
            productCard(),
            DetailRequestContext(makerName = "레노버", productName = "아이디어패드", prodType = "32741"),
        )

        assertThat(request).isNotNull
        assertThat(request!!.uri().toString()).isEqualTo("https://prod.danawa.com/info/ajax/getProductDescription.ajax.php")
        assertThat(request.headers().firstValue("Referer")).hasValue("https://prod.danawa.com/info/?pcode=123&cate=112758")
        assertThat(request.headers().firstValue("X-Requested-With")).hasValue("XMLHttpRequest")
    }

    private fun productCard(): ProductCard {
        return ProductCard(
            productCode = "123",
            productName = "테스트 노트북",
            detailPage = "https://prod.danawa.com/info/?pcode=123&cate=112758",
            imageUrl = "https://img.danawa.com/sample.jpg",
            price = 1_234_000,
            cate1 = "112",
            cate2 = "758",
            cate3 = "0",
            cate4 = "112758",
        )
    }
}
