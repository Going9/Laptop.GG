package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.CommentResult
import going9.laptopgg.application.laptop.GetLaptopDetailPageUseCase
import going9.laptopgg.application.laptop.LaptopDetailPageResult
import going9.laptopgg.application.laptop.LaptopDetailResult
import going9.laptopgg.web.dto.request.CommentRequest
import going9.laptopgg.web.dto.response.CommentResponse
import going9.laptopgg.web.dto.response.LaptopDetailResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.ui.ExtendedModelMap

class LaptopPageControllerTest {
    private val getLaptopDetailPageUseCase = Mockito.mock(GetLaptopDetailPageUseCase::class.java)
    private val controller = LaptopPageController(
        getLaptopDetailPageUseCase = getLaptopDetailPageUseCase,
    )

    @Test
    fun `laptop detail delegates to services and keeps model attributes`() {
        val laptopDetail = laptopDetailResponse(id = 10L)
        val comments = listOf(CommentResult(id = 1L, author = "iggy", content = "좋아요"))
        Mockito.`when`(getLaptopDetailPageUseCase.get(10L))
            .thenReturn(LaptopDetailPageResult(laptopDetail = laptopDetail, comments = comments))
        val model = ExtendedModelMap()

        val viewName = controller.showLaptopDetail(10L, model)

        assertThat(viewName).isEqualTo("laptop-detail")
        assertThat(model["laptopDetail"]).isEqualTo(LaptopDetailResponse.from(laptopDetail))
        assertThat(model["commentsOfLaptop"]).isEqualTo(comments.map(CommentResponse::from))
        assertThat(model["commentRequest"]).isEqualTo(CommentRequest(laptopId = 10L))
    }

    private fun laptopDetailResponse(id: Long): LaptopDetailResult {
        return LaptopDetailResult(
            id = id,
            name = "테스트 노트북",
            imageUrl = "https://example.com/laptop.jpg",
            manufacturer = "테스트",
            detailPage = "https://prod.danawa.com/info/?pcode=1&cate=112758",
            price = 1_490_000,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra",
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "1920x1200",
            brightness = 300,
            refreshRate = 60,
            ramSize = 16,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = "Intel Graphics",
            tgp = 0,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 60.0,
            storageCapacity = 512,
            storageSlotCount = 1,
            weight = 1.2,
            usage = listOf("사무/인강용"),
        )
    }
}
