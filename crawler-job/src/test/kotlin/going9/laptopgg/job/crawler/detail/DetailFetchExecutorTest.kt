package going9.laptopgg.job.crawler.detail

import going9.laptopgg.job.crawler.list.ProductCard
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DetailFetchExecutorTest {
    @Test
    fun `fetch executes detail tasks and preserves work item order`() {
        val workItems = listOf(
            DetailRefreshWorkItem(productCard = productCard("100"), existingLaptop = null),
            DetailRefreshWorkItem(productCard = productCard("200"), existingLaptop = null),
        )

        val outcomes = DetailFetchExecutor.fixed(2).use { detailFetchExecutor ->
            detailFetchExecutor.fetch(workItems) { workItem ->
                DetailRefreshOutcome(workItem = workItem)
            }
        }

        assertThat(outcomes.map { it.workItem.productCard.productCode }).containsExactly("100", "200")
    }

    @Test
    fun `close shuts down detail task executor lifecycle`() {
        val detailFetchExecutor = DetailFetchExecutor.fixed(1)

        detailFetchExecutor.close()

        assertThatThrownBy {
            detailFetchExecutor.fetch(listOf(DetailRefreshWorkItem(productCard = productCard("300"), existingLaptop = null))) {
                DetailRefreshOutcome(workItem = it)
            }
        }.isInstanceOf(RejectedExecutionException::class.java)
    }

    @Test
    fun `fatal detail task error is rethrown without execution wrapper`() {
        val error = NoClassDefFoundError("detail parser linkage")

        assertThatThrownBy {
            DetailFetchExecutor.fixed(1).use { detailFetchExecutor ->
                detailFetchExecutor.fetch(
                    listOf(DetailRefreshWorkItem(productCard = productCard("400"), existingLaptop = null)),
                ) {
                    throw error
                }
            }
        }.isSameAs(error)
    }

    @Test
    fun `fatal detail task error cancels remaining detail tasks`() {
        val error = NoClassDefFoundError("detail parser linkage")
        val slowTaskStarted = CountDownLatch(1)
        val slowTaskInterrupted = CountDownLatch(1)
        val detailFetchExecutor = DetailFetchExecutor.fixed(2)

        try {
            assertThatThrownBy {
                detailFetchExecutor.fetch(
                    listOf(
                        DetailRefreshWorkItem(productCard = productCard("fatal"), existingLaptop = null),
                        DetailRefreshWorkItem(productCard = productCard("slow"), existingLaptop = null),
                    ),
                ) { workItem ->
                    when (workItem.productCard.productCode) {
                        "fatal" -> {
                            slowTaskStarted.await(1, TimeUnit.SECONDS)
                            throw error
                        }
                        else -> {
                            slowTaskStarted.countDown()
                            try {
                                Thread.sleep(10_000)
                            } catch (exception: InterruptedException) {
                                slowTaskInterrupted.countDown()
                                throw exception
                            }
                            DetailRefreshOutcome(workItem = workItem)
                        }
                    }
                }
            }.isSameAs(error)

            assertThat(slowTaskInterrupted.await(1, TimeUnit.SECONDS)).isTrue()
        } finally {
            detailFetchExecutor.close()
        }
    }

    private fun productCard(code: String): ProductCard {
        return ProductCard(
            productCode = code,
            productName = "Laptop $code",
            detailPage = "https://prod.danawa.com/info/?pcode=$code&cate=112758",
            imageUrl = "https://img.danawa.com/$code.jpg",
            price = 1_000,
            cate1 = "112",
            cate2 = "758",
            cate3 = "0",
            cate4 = "112758",
        )
    }
}
