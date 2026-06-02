package going9.laptopgg.application.crawler.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CrawledLaptopFieldChangePolicyTest {
    private val policy = CrawledLaptopFieldChangePolicy()

    @Test
    fun `text values are trimmed and blank values are ignored`() {
        assertThat(policy.changedText("테스트", " 테스트 ")).isNull()
        assertThat(policy.changedText("테스트", " ")).isNull()
        assertThat(policy.changedText("테스트", "신규")).isEqualTo("신규")
    }

    @Test
    fun `text normalization trims required values and drops blank optional values`() {
        assertThat(policy.normalizeRequiredText(" 테스트 ")).isEqualTo("테스트")
        assertThat(policy.normalizeOptionalText(" 테스트 ")).isEqualTo("테스트")
        assertThat(policy.normalizeOptionalText(" ")).isNull()
    }

    @Test
    fun `present values change only when non null and different`() {
        assertThat(policy.changedPresent(16, null)).isNull()
        assertThat(policy.changedPresent(16, 16)).isNull()
        assertThat(policy.changedPresent(16, 32)).isEqualTo(32)
    }

    @Test
    fun `usage values are normalized and compared without order sensitivity`() {
        assertThat(policy.changedUsages(listOf("사무", "게임"), listOf(" 게임 ", "사무"))).isNull()
        assertThat(policy.changedUsages(listOf("사무"), listOf(" ", ""))).isNull()
        assertThat(policy.changedUsages(listOf("사무"), listOf("사무", "영상"))).containsExactly("사무", "영상")
    }

    @Test
    fun `usage normalization trims blanks and preserves first occurrence order`() {
        assertThat(policy.normalizeUsages(listOf(" 사무 ", "", "게임", "사무")))
            .containsExactly("사무", "게임")
    }

    @Test
    fun `update command is changed when any field is present`() {
        assertThat(policy.hasChanges(UpdateCrawledLaptopCommand())).isFalse()
        assertThat(policy.hasChanges(UpdateCrawledLaptopCommand(price = 1_000_000))).isTrue()
        assertThat(policy.hasChanges(UpdateCrawledLaptopCommand(usages = listOf("사무")))).isTrue()
    }
}
