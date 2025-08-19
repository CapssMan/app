package com.example.econcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class InvestmentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_investment, container, false)

        val calculateButton: MaterialButton = view.findViewById(R.id.calculate_button)
        val resultText: MaterialTextView = view.findViewById(R.id.result_text)

        calculateButton.setOnClickListener {
            val cashFlows = listOf(-1000000.0, 500000.0, 600000.0, 700000.0) // Пример
            val npv = EconUtils.calculateNPV(0.1, cashFlows)
            val irr = EconUtils.calculateIRR(cashFlows)
            val roi = EconUtils.calculateROI(cashFlows.sumOf { if (it > 0) it else 0.0 }, cashFlows[0])
            val payback = EconUtils.calculatePayback(cashFlows)
            val pi = EconUtils.calculatePI(0.1, cashFlows)

            resultText.text = """
                Результаты:
                - NPV: ${String.format("%.0f", npv)} руб
                - IRR: ${String.format("%.1f", irr * 100)}%
                - ROI: ${String.format("%.1f", roi * 100)}%
                - Окупаемость: ${if (payback > 0) String.format("%.1f", payback) else "не окупается"} года
                - PI: ${String.format("%.2f", pi)}
            """.trimIndent()
        }

        return view
    }
}