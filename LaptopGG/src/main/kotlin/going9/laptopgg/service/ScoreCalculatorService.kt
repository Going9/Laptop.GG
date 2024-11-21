//package going9.laptopgg.service
//
//import going9.laptopgg.domain.laptop.ColorAccuracy
//import going9.laptopgg.domain.laptop.Laptop
//import going9.laptopgg.dto.request.LaptopRecommendationRequest
//import going9.laptopgg.dto.request.PurposeDetail
//import org.springframework.stereotype.Service
//
//@Service
//class ScoreCalculatorService {
//    // 예산 점수
//    private fun calculateBudgetScore(laptop: Laptop, budget: Int): Double {
//        return 1.0 - (laptop.price.toDouble() / budget)
//    }
//
//    // 무게 점수
//    private fun calculateWeightScore(laptop: Laptop, weight: Double): Double {
//        return 1.0 - (laptop.weight / weight)
//    }
//
//    // 그래픽 관련 점수
//    fun mapGraphicScore(): Map<String, Double> {
//        val gpus = listOf(
//            "RTX4090",
//            "RTX4080",
//            "RTX4070",
//            "RTX4060",
//            "RTX4050",
//            "RTX3080 Ti",
//            "RTX3080",
//            "RTX3070 Ti",
//            "RTX3070",
//            "RTX3060",
//            "RTX3050 Ti",
//            "RTX3050"
//        )
//
//        val maxScore = 1.0
//        val minScore = 0.0
//        val step = (maxScore - minScore) / (gpus.size - 1) // 점수 간격 계산
//
//        return gpus.mapIndexed { index, gpu ->
//            gpu to (maxScore - index * step) // 순서대로 점수 부여
//        }.toMap()
//    }
//
//    private fun calculateGraphicScore(laptop: Laptop): Double {
//        if (laptop.gpus.first().gpu.isIgpu) {
//            return 0.0
//        }
//
//        val graphicScores = mapGraphicScore()
//        val minGraphicScores = laptop.gpus.minOf {
//            laptopGpu -> graphicScores[laptopGpu.gpu.name]
//            ?: throw IllegalArgumentException("GPU ${laptopGpu.gpu.name} not found in the score map")
//        }
//        return minGraphicScores
//    }
//
//    private fun calculateMuxScore(laptop: Laptop): Double {
//        if (laptop.gpus.first().gpu.isIgpu) {
//            return 0.0
//        }
//
//        val isMux = laptop.gpus[0].isMux
//        return if (isMux) { 1.0 } else 0.8
//    }
//
//    private fun calculateTgpScore(laptop: Laptop): Double {
//        if (laptop.gpus.first().gpu.isIgpu) {
//            return 0.0
//        }
//
//        val minTgp = laptop.gpus.minOf {
//            laptopGpu -> laptopGpu.tgp ?: throw IllegalArgumentException("GPU ${laptopGpu.gpu.name} has no tgp")
//        }
//
//        val minTgpGpu = laptop.gpus.first {it.tgp == minTgp}
//
//        val gpuName = minTgpGpu.gpu.name
//        val referenceTgp = when {
//            "3050 TI" in gpuName -> 60
//            "3050" in gpuName -> 80
//            "30" in gpuName -> 150
//            "40" in gpuName -> 100
//            "4080" in gpuName -> 150
//            "4090" in gpuName -> 150
//            else -> throw IllegalArgumentException("Unknown GPU series in name $gpuName")
//        }
//        val tgpScore = (minTgp.toDouble() / referenceTgp).coerceAtMost(1.0)
//        return tgpScore
//    }
//
//    // 램 관련 점수
//    private fun calculateRamSlotScore(laptop: Laptop): Double {
//        val slotCount = laptop.rams[0].slot
//        return if (slotCount == null) {0.5} else if (slotCount == 1) {0.8} else 1.0
//    }
//
//    // 디스플레이 관련 점수
//    private fun calculateDisplayScores(laptop: Laptop): Triple<Double, Double, Double> {
//        val referenceResolution = 3840 * 2160
//        val referenceRefreshRate = 240
//
//        var minResolution = Int.MAX_VALUE
//        var minRefreshRate = Int.MAX_VALUE
//        var colorAccuracyScore = 1.0
//
//        laptop.displays.forEach { display ->
//            val resolution = display.resolutionWidth * display.resolutionHeight
//            if (resolution < minResolution) minResolution = resolution
//            if (display.refreshRate < minRefreshRate) minRefreshRate = display.refreshRate
//            val accuracyScore = when (display.colorAccuracy) {
//                ColorAccuracy.BAD -> 0.5
//                ColorAccuracy.GOOD -> 0.8
//                ColorAccuracy.DESIGNER -> 1.0
//            }
//            if (accuracyScore < colorAccuracyScore) colorAccuracyScore = accuracyScore
//        }
//
//        val resolutionScore = minResolution.toDouble() / referenceResolution
//        val refreshRateScore = minRefreshRate.toDouble() / referenceRefreshRate
//        return Triple(resolutionScore, refreshRateScore, colorAccuracyScore)
//    }
//
//    // AS 점수
//    private fun calculateServiceScore(manufacturer: String): Double {
//        return if (manufacturer in listOf("SAMSUNG", "LG", "DELL", "HP")) 1.0 else 0.8
//    }
//
//    // 최종 점수 계산
//    fun calculateScore(laptop: Laptop, request: LaptopRecommendationRequest): Double {
//        val budgetScore = calculateBudgetScore(laptop, request.budget)
//        val weightScore = calculateWeightScore(laptop, request.weight)
//        val (resolutionScore, refreshRateScore, colorAccuracyScore) = calculateDisplayScores(laptop)
//        val ramSlotScore = calculateRamSlotScore(laptop)
//        val serviceScore = calculateServiceScore(laptop.manufacturer)
//        val graphicScore = calculateGraphicScore(laptop)
//        val tgpScore = calculateTgpScore(laptop)
//        val muxScore = calculateMuxScore(laptop)
//
//        return when (request.purpose) {
//            PurposeDetail.OFFICE -> {
//                (budgetScore * 0.3) + (weightScore * 0.4) + (resolutionScore * 0.1) + (serviceScore * 0.2)
//            }
//            PurposeDetail.LIGHT_OFFICE -> {
//                (budgetScore * 0.3) + (weightScore * 0.5) + (resolutionScore * 0.1) + (serviceScore * 0.2)
//            }
//            PurposeDetail.OFFICE_LOL -> {
//                (budgetScore * 0.3) + (weightScore * 0.3) + (resolutionScore * 0.1) + (serviceScore * 0.2) + (refreshRateScore * 0.2)
//            }
//            PurposeDetail.CREATOR -> {
//                (budgetScore * 0.1) + (weightScore * 0.2) + (resolutionScore * 0.1) + (serviceScore * 0.2) + (colorAccuracyScore * 0.2) + (ramSlotScore * 0.2)
//            }
//            PurposeDetail.LIGHT_GAMING -> {
//                (budgetScore * 0.1) + (weightScore * 0.4) + (muxScore * 0.1) + (graphicScore * 0.3) + (refreshRateScore * 0.1)
//            }
//            PurposeDetail.MAINSTREAM_GAMING -> {
//                (budgetScore * 0.1) + (weightScore * 0.1) + (muxScore * 0.2) + (graphicScore * 0.3) + (refreshRateScore * 0.1) + (tgpScore * 0.2)
//            }
//            PurposeDetail.HEAVY_GAMING -> {
//                (budgetScore * 0.1) + (muxScore * 0.1) + (graphicScore * 0.5) + (refreshRateScore * 0.1) + (tgpScore * 0.2)
//            }
//        }
//    }
//}
