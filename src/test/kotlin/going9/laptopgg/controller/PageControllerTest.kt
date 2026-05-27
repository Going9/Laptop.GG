package going9.laptopgg.controller

import going9.laptopgg.dto.request.CommentRequest
import going9.laptopgg.dto.request.CommentUpdateRequest
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.CommentResponse
import going9.laptopgg.dto.response.LaptopDetailResponse
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import going9.laptopgg.service.CommentService
import going9.laptopgg.service.LaptopService
import going9.laptopgg.service.RecommendationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.ui.ExtendedModelMap

class PageControllerTest {
    private val laptopService = Mockito.mock(LaptopService::class.java)
    private val recommendationService = Mockito.mock(RecommendationService::class.java)
    private val commentService = Mockito.mock(CommentService::class.java)
    private val controller = PageController(
        laptopService = laptopService,
        recommendationService = recommendationService,
        commentService = commentService,
    )

    @Test
    fun `recommendation form keeps expected model attributes`() {
        val model = ExtendedModelMap()

        val viewName = controller.showRecommendationForm(model)

        assertThat(viewName).isEqualTo("recommendation-form")
        assertThat(model["laptopRecommendationRequest"]).isInstanceOf(LaptopRecommendationRequest::class.java)
        assertThat(model["screenSizeList"]).isEqualTo(LaptopRecommendationRequest.ALL_SELECTABLE_SCREEN_SIZES)
        assertThat(model["budgetPresetList"]).isNotNull
        assertThat(model["weightPresetList"]).isNotNull
    }

    @Test
    fun `recommendation result delegates to service and keeps list model attributes`() {
        val request = LaptopRecommendationRequest.fixture()
        val pageable = PageRequest.of(0, 10)
        val recommendedLaptop = LaptopRecommendationListResponse(
            id = 1L,
            score = 91.2,
            imgLink = "https://example.com/laptop.jpg",
            price = 1_490_000,
            name = "테스트 노트북",
            manufacturer = "테스트",
            weight = 1.2,
            screenSize = 14,
            cpu = "Core Ultra",
            gpu = "Intel Graphics",
            resolutionLabel = "FHD",
            reasons = listOf("문서 작업에 잘 맞아요"),
        )
        Mockito.`when`(recommendationService.recommendLaptops(request, pageable))
            .thenReturn(PageImpl(listOf(recommendedLaptop), pageable, 1))
        val model = ExtendedModelMap()

        val viewName = controller.recommendLaptops(request, "price_asc", pageable, model)

        assertThat(viewName).isEqualTo("recommendation-list")
        assertThat(model["recommendedLaptops"]).isEqualTo(listOf(recommendedLaptop))
        assertThat(model["totalCount"]).isEqualTo(1L)
        assertThat(model["currentSort"]).isEqualTo("price_asc")
    }

    @Test
    fun `laptop detail delegates to services and keeps model attributes`() {
        val laptopDetail = laptopDetailResponse(id = 10L)
        val comments = listOf(CommentResponse(id = 1L, author = "iggy", content = "좋아요"))
        Mockito.`when`(laptopService.findLaptopById(10L)).thenReturn(laptopDetail)
        Mockito.`when`(commentService.getAllComments(10L)).thenReturn(comments)
        val model = ExtendedModelMap()

        val viewName = controller.showLaptopDetail(10L, model)

        assertThat(viewName).isEqualTo("laptop-detail")
        assertThat(model["laptopDetail"]).isEqualTo(laptopDetail)
        assertThat(model["commentsOfLaptop"]).isEqualTo(comments)
        assertThat(model["commentRequest"]).isEqualTo(CommentRequest())
    }

    @Test
    fun `comment create redirects to laptop detail after service call`() {
        val request = CommentRequest(laptopId = 3L, author = "iggy", content = "좋아요", passWord = "pw")

        val viewName = controller.addComment(request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(commentService).saveComment(request)
    }

    @Test
    fun `comment edit redirects to laptop detail after service call`() {
        val request = CommentRequest(laptopId = 3L, author = "iggy", content = "수정", passWord = "pw")

        val viewName = controller.editComment(7L, request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(commentService).updateComment(
            7L,
            CommentUpdateRequest(passWord = "pw", content = "수정"),
        )
    }

    private fun laptopDetailResponse(id: Long): LaptopDetailResponse {
        return LaptopDetailResponse(
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
