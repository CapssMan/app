package com.example.econcalc

object EconUtils {

    fun calculateAnnuityPayment(principal: Double, rate: Double, periods: Int): Double {
        val monthlyRate = rate / 12
        return principal * (monthlyRate * (1 + monthlyRate).pow(periods)) / ((1 + monthlyRate).pow(periods) - 1)
    }

    fun calculateDifferentiatedPayment(principal: Double, rate: Double, totalPeriods: Int, currentPeriod: Int): Double {
        val monthlyRate = rate / 12
        val remainingPeriods = totalPeriods - currentPeriod + 1
        return principal / totalPeriods + principal * (remainingPeriods.toDouble() / totalPeriods) * monthlyRate
    }

    fun calculateNPV(rate: Double, cashFlows: List<Double>): Double {
        var npv = 0.0
        cashFlows.forEachIndexed { index, cashFlow ->
            npv += cashFlow / (1 + rate).pow(index.toDouble())
        }
        return npv
    }

    fun calculateIRR(cashFlows: List<Double>): Double {
        var irr = 0.1
        val epsilon = 0.0001
        var npv: Double
        do {
            npv = calculateNPV(irr, cashFlows)
            val derivative = cashFlows.mapIndexed { index, cashFlow ->
                -index * cashFlow / (1 + irr).pow(index + 1.0)
            }.sum()
            irr -= npv / derivative
        } while (npv.absoluteValue > epsilon)
        return irr
    }

    fun calculateROI(totalIncome: Double, initialInvestment: Double): Double {
        return totalIncome / initialInvestment
    }

    fun calculatePayback(cashFlows: List<Double>): Double {
        var cumulative = 0.0
        for (i in 1 until cashFlows.size) {
            cumulative += cashFlows[i]
            if (cumulative >= -cashFlows[0]) {
                return i - 1 + (-cashFlows[0] - (cumulative - cashFlows[i])) / cashFlows[i]
            }
        }
        return -1.0
    }

    fun calculatePropertyTaxDeduction(propertyCost: Double): Double {
        return minOf(propertyCost * 0.13, 260_000.0)
    }

    fun calculateInterestTaxDeduction(totalInterest: Double): Double {
        return minOf(totalInterest * 0.13, 390_000.0)
    }
}