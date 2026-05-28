package going9.laptopgg.job.crawler

data class CrawlerAttributeFilter(
    val name: String,
    val value: String,
)

object CrawlerFilterSets {
    val coreCpuCodenames = listOf(
        CrawlerAttributeFilter("팬서레이크", "758|6492|1137658|OR"),
        CrawlerAttributeFilter("루나레이크", "758|6492|984997|OR"),
        CrawlerAttributeFilter("메테오레이크", "758|6492|928801|OR"),
        CrawlerAttributeFilter("애로우레이크", "758|6492|1016359|OR"),
        CrawlerAttributeFilter("랩터레이크-R", "758|6492|938068|OR"),
        CrawlerAttributeFilter("랩터레이크", "758|6492|823300|OR"),
        CrawlerAttributeFilter("고르곤 포인트", "758|6492|1137661|OR"),
        CrawlerAttributeFilter("크라켄 포인트", "758|6492|1022068|OR"),
        CrawlerAttributeFilter("스트릭스 헤일로", "758|6492|1020355|OR"),
        CrawlerAttributeFilter("스트릭스 포인트", "758|6492|987658|OR"),
        CrawlerAttributeFilter("파이어 레인지", "758|6492|1025521|OR"),
        CrawlerAttributeFilter("호크포인트", "758|6492|929143|OR"),
        CrawlerAttributeFilter("드래곤 레인지", "758|6492|823528|OR"),
        CrawlerAttributeFilter("피닉스", "758|6492|823516|OR"),
        CrawlerAttributeFilter("오라이온", "758|6492|984553|OR"),
        CrawlerAttributeFilter("오라이온 3세대", "758|6492|1144288|OR"),
    )

    val extendedCpuCodenames = buildList {
        addAll(coreCpuCodenames)
        add(CrawlerAttributeFilter("트윈레이크", "758|6492|1049362|OR"))
        add(CrawlerAttributeFilter("엘더레이크-N", "758|6492|845353|OR"))
        add(CrawlerAttributeFilter("램브란트-R", "758|6492|823525|OR"))
        add(CrawlerAttributeFilter("램브란트", "758|6492|758011|OR"))
        add(CrawlerAttributeFilter("바르셀로-R", "758|6492|823522|OR"))
        add(CrawlerAttributeFilter("바르셀로", "758|6492|762679|OR"))
        add(CrawlerAttributeFilter("세잔", "758|6492|713959|OR"))
        add(CrawlerAttributeFilter("루시엔", "758|6492|713962|OR"))
        add(CrawlerAttributeFilter("멘도시노", "758|6492|817711|OR"))
    }
}
