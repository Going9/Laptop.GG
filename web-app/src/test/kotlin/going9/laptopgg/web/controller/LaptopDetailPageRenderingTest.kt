package going9.laptopgg.web.controller

import going9.laptopgg.application.comment.CommentResult
import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.application.laptop.LaptopDetailResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LaptopDetailPageRenderingTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var getLaptopDetailUseCase: GetLaptopDetailUseCase

    @MockitoBean
    lateinit var manageCommentUseCase: ManageCommentUseCase

    @Test
    fun `laptop detail page renders comment create list edit and delete surface`() {
        Mockito.`when`(getLaptopDetailUseCase.get(10L)).thenReturn(laptopDetailResult(id = 10L))
        Mockito.`when`(manageCommentUseCase.listByLaptop(10L)).thenReturn(
            listOf(CommentResult(id = 1L, author = "iggy", content = "좋아요")),
        )

        val html = mockMvc.perform(get("/laptops/10"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        assertThat(html).contains(
            "사용자 의견",
            "댓글 남기기",
            "좋아요",
            "/comments/1/edit",
            "/comments/1/delete",
        )
        assertThat(Regex("""name="laptopId"""").findAll(html).count()).isEqualTo(1)
    }

    private fun laptopDetailResult(id: Long): LaptopDetailResult {
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
