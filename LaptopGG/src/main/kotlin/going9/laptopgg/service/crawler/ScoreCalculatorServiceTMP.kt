package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.ColorAccuracy
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.NewLaptop
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.request.PurposeDetail
import org.springframework.stereotype.Service

@Service
class ScoreCalculatorServiceTMP {
    // 예산 점수
    private fun calculateBudgetScore(newLaptop: NewLaptop, budget: Int): Double {
        return 1.0 - (newLaptop.price!!.toDouble() / budget)
    }

    // 무게 점수
    private fun calculateWeightScore(newLaptop: NewLaptop, weight: Double): Double {
        return 1.0 - (newLaptop.weight!! / weight)
    }

    // 그래픽 관련 점수
    fun mapGraphicScore(): Map<String, Double> {
        val gpus = listOf(
            "RTX4090",
            "RTX4080",
            "RTX4070",
            "RTX4060",
            "RTX4050",
            "RTX3080 Ti",
            "RTX3080",
            "RTX3070 Ti",
            "RTX3070",
            "RTX3060",
            "RTX3050 Ti",
            "RTX3050"
        )

        val maxScore = 1.0
        val minScore = 0.0
        val step = (maxScore - minScore) / (gpus.size - 1) // 점수 간격 계산

        return gpus.mapIndexed { index, gpu ->
            gpu to (maxScore - index * step) // 순서대로 점수 부여
        }.toMap()
    }

    private fun calculateGraphicScore(laptop: Laptop): Double {
        val graphicScores = mapGraphicScore()
        val minGraphicScores = laptop.gpus.minOf {
                laptopGpu -> graphicScores[laptopGpu.gpu.name]
            ?: throw IllegalArgumentException("GPU ${laptopGpu.gpu.name} not found in the score map")
        }
        return minGraphicScores
    }

    private fun calculateMuxScore(laptop: Laptop): Double {
        if (laptop.gpus.first().gpu.isIgpu) {
            return 0.0
        }

        val isMux = laptop.gpus[0].isMux
        return if (isMux) { 1.0 } else 0.8
    }

    private fun calculateTgpScore(laptop: NewLaptop): Double {
        if (laptop.tgp != null && laptop.tgp != 0) {
            val gpuName = laptop.graphicsType!!
            val referenceTgp = when {
                "RTX 2000 Ada" in gpuName -> 80
                "RTX 3500 Ada" in gpuName -> 80
                "RTX 5000 Ada" in gpuName -> 110
                "A1000" in gpuName -> 50
                "2050" in gpuName -> 30
                "3050" in gpuName -> 80
                "3050 TI" in gpuName -> 60
                "3060" in gpuName -> 120
                "3070" in gpuName -> 130
                "3070 TI" in gpuName -> 130
                "3080" in gpuName -> 140
                "3080 Ti" in gpuName -> 140
                "4050" in gpuName -> 80
                "4060" in gpuName -> 100
                "4070" in gpuName -> 120
                "4080" in gpuName -> 150
                "4090" in gpuName -> 150
                "라데온 RX 6600M" in gpuName -> 80
                "라데온 RX 6500M" in gpuName -> 80
                else -> return 0.5
            }
            return (laptop.tgp!!.toDouble()!! / referenceTgp).coerceAtMost(1.0)
        } else {
            return 0.5
        }
    }

    // 램 관련 점수
    private fun calculateRamSlotScore(laptop: NewLaptop): Double {
        val slotCount = laptop.isRamReplaceable
        return if (slotCount == true) {1.0} else 0.5
    }

    // 디스플레이 관련 점수
    private fun calculateDisplayScores(laptop: NewLaptop): Pair<Double, Double> {
        val referenceResolution = 3840 * 2160
        val referenceRefreshRate = 240
        val resolutionScore = if (laptop.resolution != null) {
            val resolutionPattern = Regex("([0-9]+)x([0-9]+)")
            val match = resolutionPattern.find(laptop.resolution)
            if (match != null) {
                val width = match.groupValues[1].toInt()
                val height = match.groupValues[2].toInt()
                (width * height).toDouble() / referenceResolution
            } else {
                0.25 // 기본값
            }
        } else {
            0.25 // 기본값
        }
        val refreshRateScore = if (laptop.refreshRate != null) {
            laptop.refreshRate.toDouble() / referenceRefreshRate
        } else {
            0.25
        }


        return Pair(resolutionScore, refreshRateScore)
    }

    // AS 점수
    private fun calculateServiceScore(manufacturer: String): Double {
        return if (manufacturer in listOf("삼성전자", "LG전자", "DELL", "HP")) 1.0 else 0.8
    }

    // 배터리 점수
    private fun calculateBatteryScore(laptop: NewLaptop): Double {
        val referenceBatteryCapacity = 100.0
        val batteryScore = if (laptop.batteryCapacity != null) {
            laptop.batteryCapacity / referenceBatteryCapacity
        } else {
            0.4
        }
        return batteryScore
    }


    // 최종 점수 계산
    fun calculateScore(laptop: NewLaptop, request: LaptopRecommendationRequest): Double {
        val budgetScore = calculateBudgetScore(laptop, request.budget)
        val weightScore = calculateWeightScore(laptop, request.weight)
        val (resolutionScore, refreshRateScore) = calculateDisplayScores(laptop)
        val ramSlotScore = calculateRamSlotScore(laptop)
        val serviceScore = calculateServiceScore(laptop.name.substringBefore(" "))
        val tgpScore = calculateTgpScore(laptop)
        val batteryScore = calculateBatteryScore(laptop)

        return when (request.purpose) {
            PurposeDetail.OFFICE -> {
                (budgetScore * 0.3) + (weightScore * 0.4) + (resolutionScore * 0.1) + (serviceScore * 0.2)
            }
            PurposeDetail.LONG_BATTERY -> {
                (batteryScore * 0.5) + (budgetScore * 0.3) + (weightScore * 0.2)
            }
            PurposeDetail.LIGHT_OFFICE -> {
                (budgetScore * 0.3) + (weightScore * 0.5) + (resolutionScore * 0.1) + (serviceScore * 0.2)
            }
            PurposeDetail.OFFICE_LOL -> {
                (budgetScore * 0.3) + (weightScore * 0.3) + (resolutionScore * 0.1) + (serviceScore * 0.2) + (refreshRateScore * 0.2)
            }
            PurposeDetail.CREATOR -> {
                (budgetScore * 0.1) + (weightScore * 0.2) + (resolutionScore * 0.3) + (serviceScore * 0.2) + (ramSlotScore * 0.2)
            }
            PurposeDetail.LIGHT_GAMING -> {
                (budgetScore * 0.3) + (weightScore * 0.5) + (refreshRateScore * 0.2)
            }
            PurposeDetail.MAINSTREAM_GAMING -> {
                (budgetScore * 0.3) + (weightScore * 0.2) + (refreshRateScore * 0.2) + (tgpScore * 0.3)
            }
            PurposeDetail.HEAVY_GAMING -> {
                (budgetScore * 0.2) + (refreshRateScore * 0.2) + (tgpScore * 0.6)
            }
        }
    }
}