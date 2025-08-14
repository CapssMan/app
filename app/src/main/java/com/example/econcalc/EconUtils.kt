package com.example.econcalc

import kotlin.math.pow

object EconUtils {

    fun calculateNPV(rate: Double, cashFlows: List<Double>): Double {
        var npv = 0.0
        for (i in cashFlows.indices) {
            npv += cashFlows[i] / (1 + rate).pow(i.toDouble())
        }
        return npv
    }

    fun calculateIRR(cashFlows: List<Double>): Double {
        var min = 0.0
        var max = 1.0
        var guess = 0.0
        for (i in 0..100) {
            guess = (min + max) / 2
            val npv = calculateNPV(guess, cashFlows)
            if (npv > 0) {
                min = guess
            } else {
                max = guess
            }
            if (kotlin.math.abs(npv) < 0.0001) break
        }
        return guess
    }

    fun calculateMIRR(cashFlows: List<Double>, financeRate: Double, reinvestRate: Double): Double {
        val n = cashFlows.size - 1
        var positiveFlows = 0.0
        var negativeFlows = 0.0

        for (i in 1..n) {
            if (cashFlows[i] > 0) {
                positiveFlows += cashFlows[i] * (1 + reinvestRate).pow((n - i).toDouble())
            } else {
                negativeFlows += kotlin.math.abs(cashFlows[i]) / (1 + financeRate).pow(i.toDouble())
            }
        }
        return (positiveFlows / negativeFlows).pow(1.0 / n) - 1
    }

    fun calculatePI(rate: Double, cashFlows: List<Double>): Double {
        val initial = kotlin.math.abs(cashFlows[0])
        val pvInflows = cashFlows.drop(1).fold(0.0) { acc, cf ->
            acc + cf / (1 + rate).pow(cashFlows.indexOf(cf).toDouble())
        }
        return pvInflows / initial
    }

    fun calculatePayback(cashFlows: List<Double>): Double {
        var cumulative = 0.0
        for (i in 1 until cashFlows.size) {
            cumulative += cashFlows[i]
            if (cumulative >= kotlin.math.abs(cashFlows[0])) {
                return i - 1 + (kotlin.math.abs(cashFlows[0]) - (cumulative - cashFlows[i])) / cashFlows[i]
            }
        }
        return -1.0 // Not payback
    }

    fun calculateDiscountedPayback(rate: Double, cashFlows: List<Double>): Double {
        var cumulative = 0.0
        for (i in 1 until cashFlows.size) {
            cumulative += cashFlows[i] / (1 + rate).pow(i.toDouble())
            if (cumulative >= kotlin.math.abs(cashFlows[0])) {
                return i - 1 + (kotlin.math.abs(cashFlows[0]) - (cumulative - cashFlows[i] / (1 + rate).pow(i.toDouble()))) / (cashFlows[i] / (1 + rate).pow(i.toDouble()))
            }
        }
        return -1.0
    }

    // Annuity payment for credit
    fun calculateAnnuityPayment(principal: Double, rate: Double, periods: Int): Double {
        val r = rate / 12 // Monthly
        return principal * r / (1 - (1 + r).pow(-periods.toDouble()))
    }

    // Differentiated payment (example for month)
    fun calculateDifferentiatedPayment(principal: Double, rate: Double, periods: Int, month: Int): Double {
        val monthlyPrincipal = principal / periods
        val remaining = principal - monthlyPrincipal * (month - 1)
        return monthlyPrincipal + remaining * (rate / 12)
    }

    // Loan schedule: return list of pairs (payment, interest, principal, balance)
    fun generateLoanSchedule(principal: Double, rate: Double, periods: Int, isAnnuity: Boolean): List<LoanPayment> {
        val schedule = mutableListOf<LoanPayment>()
        var balance = principal
        val monthlyRate = rate / 12
        val annuity = if (isAnnuity) calculateAnnuityPayment(principal, rate, periods) else 0.0

        for (month in 1..periods) {
            val interest = balance * monthlyRate
            val payment = if (isAnnuity) annuity else calculateDifferentiatedPayment(principal, rate, periods, month)
            val principalPaid = payment - interest
            balance -= principalPaid
            schedule.add(LoanPayment(payment, interest, principalPaid, balance))
        }
        return schedule
    }

    // Effective rate (APR)
    fun calculateAPR(nominalRate: Double, periods: Int): Double {
        return (1 + nominalRate / periods).pow(periods.toDouble()) - 1
    }

    // Stock yield
    fun calculateStockYield(currentPrice: Double, purchasePrice: Double): Double {
        return (currentPrice - purchasePrice) / purchasePrice
    }

    // Bond yield to maturity (approx)
    fun calculateBondYTM(faceValue: Double, coupon: Double, currentPrice: Double, years: Int): Double {
        return (coupon + (faceValue - currentPrice) / years) / ((faceValue + currentPrice) / 2)
    }

    // Dividend yield
    fun calculateDividendYield(dividend: Double, price: Double): Double {
        return dividend / price
    }

    // VAT add/subtract
    fun addVAT(price: Double, vatRate: Double): Double {
        return price * (1 + vatRate)
    }

    fun subtractVAT(priceWithVAT: Double, vatRate: Double): Double {
        return priceWithVAT / (1 + vatRate)
    }

    // Net profit after tax
    fun calculateNetProfit(revenue: Double, costs: Double, taxRate: Double, vatRate: Double): Double {
        val profitBeforeTax = revenue - costs - (revenue * vatRate)
        return profitBeforeTax * (1 - taxRate)
    }

    // Amortization (straight line)
    fun calculateAmortization(cost: Double, salvage: Double, life: Int): Double {
        return (cost - salvage) / life
    }

    // Payroll (FOT)
    fun calculatePayroll(salary: Double, taxRate: Double, contributions: Double): Double {
        return salary + salary * taxRate + contributions
    }

    // Cost of production
    fun calculateCostOfProduction(raw: Double, labor: Double, overhead: Double): Double {
        return raw + labor + overhead
    }

    // Marginal profit
    fun calculateMarginalProfit(revenue: Double, variableCosts: Double): Double {
        return revenue - variableCosts
    }

    // Break-even point
    fun calculateBreakEven(fixedCosts: Double, pricePerUnit: Double, variableCostPerUnit: Double): Double {
        return fixedCosts / (pricePerUnit - variableCostPerUnit)
    }

    // Scenario analysis (example for NPV)
    fun scenarioAnalysis(cashFlows: List<Double>, rates: List<Double>): List<Double> {
        return rates.map { calculateNPV(it, cashFlows) }
    }

    // ... Add CSV import/export using Android storage
    // For currency, use Retrofit call in a coroutine
    // Example:
    // interface CurrencyApi {
    //     @GET("latest?access_key=YOUR_API_KEY")
    //     suspend fun getRates(): Map<String, Any>
    // }
    // Use in fragment with viewLifecycleOwner.lifecycleScope.launch { ... }

}

data class LoanPayment(val payment: Double, val interest: Double, val principal: Double, val balance: Double)