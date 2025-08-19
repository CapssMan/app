package com.example.econcalc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File

class StartupFragment : Fragment() {

    private lateinit var exportPdfButton: MaterialButton
    private lateinit var resultText: MaterialTextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_startup, container, false)

        // Поля ввода
        val initialInvestmentInput: TextInputEditText = view.findViewById(R.id.initial_investment_input)
        val developmentInput: TextInputEditText = view.findViewById(R.id.development_input)
        val marketingInput: TextInputEditText = view.findViewById(R.id.marketing_input)
        val salariesInput: TextInputEditText = view.findViewById(R.id.salaries_input)
        val rentInput: TextInputEditText = view.findViewById(R.id.rent_input)
        val clientsYear1Input: TextInputEditText = view.findViewById(R.id.clients_year1_input)
        val clientsYear2Input: TextInputEditText = view.findViewById(R.id.clients_year2_input)
        val avgCheckInput: TextInputEditText = view.findViewById(R.id.avg_check_input)
        val discountRateInput: TextInputEditText = view.findViewById(R.id.discount_rate_input)
        val taxRateInput: TextInputEditText = view.findViewById(R.id.tax_rate_input)
        val calculationPeriodInput: TextInputEditText = view.findViewById(R.id.calculation_period_input)
        val calculateButton: MaterialButton = view.findViewById(R.id.calculate_button)
        val saasTemplateButton: MaterialButton = view.findViewById(R.id.saas_template_button)
        exportPdfButton = view.findViewById(R.id.export_pdf_button)
        resultText = view.findViewById(R.id.result_text)
        val cashFlowChart: LineChart = view.findViewById(R.id.cash_flow_chart)

        // Автозаполнение для SaaS
        saasTemplateButton.setOnClickListener {
            initialInvestmentInput.setText("500000")
            developmentInput.setText("300000")
            marketingInput.setText("200000")
            salariesInput.setText("150000")
            rentInput.setText("50000")
            clientsYear1Input.setText("100")
            clientsYear2Input.setText("500")
            avgCheckInput.setText("1000")
            discountRateInput.setText("0.15")
            taxRateInput.setText("0.2")
            calculationPeriodInput.setText("5")
        }

        calculateButton.setOnClickListener {
            // Получение данных
            val initialInvestment = initialInvestmentInput.text.toString().toDoubleOrNull() ?: 0.0
            val development = developmentInput.text.toString().toDoubleOrNull() ?: 0.0
            val marketing = marketingInput.text.toString().toDoubleOrNull() ?: 0.0
            val salaries = salariesInput.text.toString().toDoubleOrNull() ?: 0.0
            val rent = rentInput.text.toString().toDoubleOrNull() ?: 0.0
            val clientsYear1 = clientsYear1Input.text.toString().toIntOrNull() ?: 0
            val clientsYear2 = clientsYear2Input.text.toString().toIntOrNull() ?: 0
            val avgCheck = avgCheckInput.text.toString().toDoubleOrNull() ?: 0.0
            val discountRate = discountRateInput.text.toString().toDoubleOrNull() ?: 0.15
            val taxRate = taxRateInput.text.toString().toDoubleOrNull() ?: 0.2
            val calculationPeriod = calculationPeriodInput.text.toString().toIntOrNull() ?: 5

            // Рассчёт денежных потоков (SaaS модель: подписка)
            val cashFlows = mutableListOf<Double>()
            val totalInvestment = initialInvestment + development + marketing
            cashFlows.add(-totalInvestment) // Год 0: инвестиции
            val monthlyExpenses = salaries + rent
            var totalRevenue = 0.0

            // Прогноз доходов: линейный рост клиентов от Год 1 до Год 2, затем стабилизация
            for (year in 1..calculationPeriod) {
                val clients = if (year <= 2) {
                    clientsYear1 + ((clientsYear2 - clientsYear1) * (year - 1)) / 1
                } else {
                    clientsYear2 // Стабилизация после 2-го года
                }
                val revenue = clients * avgCheck * 12 // Годовой доход
                totalRevenue += revenue
                val netIncome = revenue - (revenue * taxRate) - monthlyExpenses * 12
                cashFlows.add(netIncome)
            }

            // Расчёты
            val npv = EconUtils.calculateNPV(discountRate, cashFlows)
            val irr = EconUtils.calculateIRR(cashFlows)
            val payback = EconUtils.calculatePayback(cashFlows)
            val runway = totalInvestment / monthlyExpenses // Runway в месяцах
            val roi = EconUtils.calculateROI(totalRevenue, totalInvestment)
            val breakEvenMonths = EconUtils.calculateBreakEven(totalInvestment, avgCheck, monthlyExpenses / clientsYear1)
            val cashBurnRate = monthlyExpenses - (clientsYear1 * avgCheck) // Кэшберн на основе 1-го года

            // Вывод результатов
            resultText.text = """
                Результаты расчёта:
                - NPV: ${String.format("%.0f", npv)} руб
                - IRR: ${String.format("%.1f", irr * 100)}%
                - Окупаемость: ${if (payback > 0) String.format("%.1f", payback) else "не окупается"} года
                - Runway: ${String.format("%.1f", runway)} месяца ${if (runway < 12) "(риск нехватки денег)" else ""}
                - ROI: ${String.format("%.0f", roi * 100)}% за $calculationPeriod лет
                - Точка безубыточности: ${if (breakEvenMonths > 0) String.format("%.1f", breakEvenMonths) else "Невозможно"} месяцев
                - Кэшберн-рейт: ${String.format("%.2f", cashBurnRate)} ₽/мес
                Совет: IRR ${String.format("%.2f", irr * 100)}% — ${if (irr > 0.2) "выше среднего рынка, но 50% стартапов не доживают до 3-го года." else "ниже среднего. Рассмотрите оптимизацию затрат."}
            """.trimIndent()
            resultText.setTextColor(ContextCompat.getColor(requireContext(), if (npv < 0) android.R.color.holo_red_dark else android.R.color.black))

            // График денежных потоков
            val entries = cashFlows.mapIndexed { index, value -> Entry(index.toFloat(), value.toFloat()) }
            val dataSet = LineDataSet(entries, "Денежные потоки")
            dataSet.color = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            dataSet.setDrawValues(false)
            cashFlowChart.data = LineData(dataSet)
            cashFlowChart.description.text = "Годы"
            cashFlowChart.invalidate()
        }

        exportPdfButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                return@setOnClickListener
            }
            val initialInvestment = initialInvestmentInput.text.toString().toDoubleOrNull() ?: 0.0
            val result = resultText.text.toString()
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Startup_Report_${System.currentTimeMillis()}.pdf")
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            document.add(Paragraph("Отчёт по стартапу"))
            document.add(Paragraph("Первоначальные инвестиции: $initialInvestment ₽"))
            document.add(Paragraph(result))
            document.close()
            resultText.text = "PDF сохранён в ${file.absolutePath}"
        }

        return view
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportPdfButton.performClick()
        } else {
            resultText.text = "Разрешение на запись в хранилище отклонено"
        }
    }
}