package com.example.econcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

class InvestmentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_investment, container, false)

        val rateInput: TextInputEditText = view.findViewById(R.id.rate_input)
        val cashFlowsInput: TextInputEditText = view.findViewById(R.id.cash_flows_input)
        val calculateButton: MaterialButton = view.findViewById(R.id.calculate_button)
        val resultText: MaterialTextView = view.findViewById(R.id.result_text)

        calculateButton.setOnClickView {
            val rate = rateInput.text.toString().toDoubleOrNull() ?: 0.0
            val cashFlowsStr = cashFlowsInput.text.toString()
            val cashFlows = cashFlowsStr.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }

            val npv = EconUtils.calculateNPV(rate, cashFlows)
            val irr = EconUtils.calculateIRR(cashFlows)
            val mirr = EconUtils.calculateMIRR(cashFlows, rate, rate) // Assume financeRate = reinvestRate = rate
            val pi = EconUtils.calculatePI(rate, cashFlows)
            val payback = EconUtils.calculatePayback(cashFlows)
            val discountedPayback = EconUtils.calculateDiscountedPayback(rate, cashFlows)

            resultText.text = "NPV: $npv\nIRR: $irr\nMIRR: $mirr\nPI: $pi\nPayback: $payback\nDiscounted Payback: $discountedPayback"
        }

        // Add graph view
        val lineChart: com.github.philjay.mpandroidchart.charts.LineChart = view.findViewById(R.id.chart)
        // Configure chart with cash flows data...

        return view
    }
}