package com.example.econcalc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.econcalc.databinding.FragmentRealEstateBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RealEstateFragment : Fragment() {

    private var _binding: FragmentRealEstateBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            binding.exportExcelButton.performClick()
            binding.exportPdfButton.performClick()
        } else {
            binding.resultText.text = "Разрешение на запись в хранилище отклонено"
            binding.resultText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRealEstateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка спиннеров
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.property_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.propertyTypeSpinner.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.payment_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.paymentTypeSpinner.adapter = adapter
        }

        // Загрузка ставок ЦБ
        binding.loadRatesButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val url = URL("https://www.cbr.ru/scripts/XML_dynamic.asp?date_req1=01/01/2025&date_req2=18/08/2025&VAL_NM_RQ=R01235")
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(url.openStream(), null)
                    var keyRate = 18.0
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.START_TAG && parser.name == "Value") {
                            keyRate = parser.nextText().replace(",", ".").toDoubleOrNull() ?: 18.0
                        }
                        parser.next()
                    }
                    requireActivity().runOnUiThread {
                        binding.mortgageRateInput.setText(keyRate.toString())
                        binding.inflationInput.setText("4.5")
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        binding.resultText.text = "Ошибка сети: ${e.message}"
                        binding.resultText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    }
                }
            }
        }

        // Пессимистичный сценарий
        binding.pessimisticButton.setOnClickListener {
            binding.marketGrowthInput.setText("-5.0")
            val currentRent = binding.rentIncomeInput.text.toString().toDoubleOrNull() ?: 0.0
            binding.rentIncomeInput.setText((currentRent * 0.8).toString())
        }

        // Оптимистичный сценарий
        binding.optimisticButton.setOnClickListener {
            binding.marketGrowthInput.setText("10.0")
            val currentRent = binding.rentIncomeInput.text.toString().toDoubleOrNull() ?: 0.0
            binding.rentIncomeInput.setText((currentRent * 1.2).toString())
        }

        // Расчёты
        binding.calculateButton.setOnClickListener {
            val propertyCost = binding.propertyCostInput.text.toString().toDoubleOrNull() ?: 0.0
            var downPayment = binding.downPaymentInput.text.toString().toDoubleOrNull() ?: 0.0
            if (downPayment < 1) downPayment *= propertyCost
            val ownershipPeriod = binding.ownershipPeriodInput.text.toString().toIntOrNull() ?: 5
            val mortgageRate = binding.mortgageRateInput.text.toString().toDoubleOrNull()?.div(100) ?: 0.18
            val mortgageTerm = binding.mortgageTermInput.text.toString().toIntOrNull() ?: 20
            val isAnnuity = binding.paymentTypeSpinner.selectedItem.toString() == "Аннуитетный"
            val rentIncome = binding.rentIncomeInput.text.toString().toDoubleOrNull() ?: 0.0
            val utilities = binding.utilitiesInput.text.toString().toDoubleOrNull() ?: 0.0
            val taxRate = binding.taxRateInput.text.toString().toDoubleOrNull()?.div(100) ?: 0.13
            val capExpenses = binding.capExpensesInput.text.toString().toDoubleOrNull() ?: 0.0
            val inflation = binding.inflationInput.text.toString().toDoubleOrNull()?.div(100) ?: 0.045
            val marketGrowth = binding.marketGrowthInput.text.toString().toDoubleOrNull()?.div(100) ?: 0.0

            val loanAmount = propertyCost - downPayment
            val monthlyMortgage = if (isAnnuity) {
                EconUtils.calculateAnnuityPayment(loanAmount, mortgageRate, mortgageTerm * 12)
            } else {
                EconUtils.calculateDifferentiatedPayment(loanAmount, mortgageRate, mortgageTerm * 12, 1)
            }

            val cashFlows = mutableListOf<Double>()
            cashFlows.add(-downPayment)
            var totalInterestPaid = 0.0
            for (year in 1..ownershipPeriod) {
                val annualRent = rentIncome * 12 * (1 + inflation).pow(year - 1)
                val annualMortgage = monthlyMortgage * 12
                val netIncome = annualRent - utilities * 12 - capExpenses - annualMortgage
                val taxOnIncome = if (netIncome > 0) netIncome * taxRate else 0.0
                val yearFlow = netIncome - taxOnIncome
                cashFlows.add(yearFlow)
                totalInterestPaid += if (isAnnuity) {
                    annualMortgage - (loanAmount / (mortgageTerm * 12) * 12)
                } else {
                    EconUtils.calculateDifferentiatedPayment(loanAmount, mortgageRate, mortgageTerm * 12, year * 12) * 12 - (loanAmount / mortgageTerm)
                }
            }

            val salePrice = propertyCost * (1 + marketGrowth).pow(ownershipPeriod.toDouble())
            cashFlows[cashFlows.lastIndex] += salePrice - loanAmount

            val npv = EconUtils.calculateNPV(mortgageRate, cashFlows)
            val irr = EconUtils.calculateIRR(cashFlows)
            val roi = EconUtils.calculateROI(cashFlows.sumOf { if (it > 0) it else 0.0 }, downPayment)
            val payback = EconUtils.calculatePayback(cashFlows)
            val annualCashFlow = cashFlows.drop(1).average()
            val propertyTaxDeduction = EconUtils.calculatePropertyTaxDeduction(propertyCost)
            val interestDeduction = EconUtils.calculateInterestTaxDeduction(totalInterestPaid)

            val rentAlternative = downPayment * (mortgageRate + 0.02)

            binding.resultText.text = """
                Результаты расчёта:
                - Годовой Cash Flow: ${String.format("%.0f", annualCashFlow)} руб
                - ROI: ${String.format("%.1f", roi * 100)}%
                - IRR: ${String.format("%.1f", irr * 100)}% (при ставке дисконтирования ${String.format("%.0f", mortgageRate * 100)}%)
                - NPV: ${String.format("%.0f", npv)} руб ${if (npv > 0) "(выгодна)" else "(убыточна)"}
                - Окупаемость: ${if (payback > 0) String.format("%.1f", payback) else "не окупается"} года
                - Налоговый вычет: ${String.format("%.0f", propertyTaxDeduction)} руб + ${String.format("%.0f", interestDeduction / ownershipPeriod)} руб/год
                - Сравнение с арендой: Альтернатива - ${String.format("%.0f", rentAlternative)} руб/год дохода от депозита
                - Рекомендация: Инвестиция ${if (irr > mortgageRate) "выгодна (IRR > депозитов)" else "невыгодна"}, но учтите риски простоя аренды
            """.trimIndent()
            binding.resultText.setTextColor(ContextCompat.getColor(requireContext(), if (npv < 0) android.R.color.holo_red_dark else android.R.color.black))

            val entries = cashFlows.mapIndexed { index, value -> Entry(index.toFloat(), value.toFloat()) }
            val dataSet = LineDataSet(entries, "Денежные потоки")
            dataSet.color = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            dataSet.setDrawValues(false)
            binding.cashFlowChart.data = LineData(dataSet)
            binding.cashFlowChart.description.text = "Годы"
            binding.cashFlowChart.invalidate()
        }

        // Экспорт в Excel
        binding.exportExcelButton.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
                return@setOnClickListener
            }

            val wb = XSSFWorkbook()
            val sheet = wb.createSheet("RealEstate Model")
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Год")
            headerRow.createCell(1).setCellValue("Денежный поток (руб)")

            val cashFlows = mutableListOf<Double>().apply {
                var downPayment = binding.downPaymentInput.text.toString().toDoubleOrNull() ?: 0.0
                val propertyCost = binding.propertyCostInput.text.toString().toDoubleOrNull() ?: 0.0
                if (downPayment < 1) downPayment *= propertyCost
                add(-downPayment)
                for (year in 1..(binding.ownershipPeriodInput.text.toString().toIntOrNull() ?: 5)) {
                    val annualRent = (binding.rentIncomeInput.text.toString().toDoubleOrNull() ?: 0.0) * 12 * (1 + (binding.inflationInput.text.toString().toDoubleOrNull()?.div(100) ?: 0.045)).pow(year - 1)
                    val annualMortgage = (if (binding.paymentTypeSpinner.selectedItem.toString() == "Аннуитетный") {
                        EconUtils.calculateAnnuityPayment(
                            propertyCost - downPayment,
                            binding.mortgageRateInput.text.toString().toDoubleOrNull()?.div(100) ?: 0.18,
                            (binding.mortgageTermInput.text.toString().toIntOrNull() ?: 20) * 12
                        )
                    } else {
                        EconUtils.calculateDifferentiatedPayment(
                            propertyCost - downPayment,
                            binding.mortgageRateInput.text.toString().toDoubleOrNull()?.div(100) ?: 0.18,
                            (binding.mortgageTermInput.text.toString().toIntOrNull() ?: 20) * 12,
                            year * 12
                        )
                    }) * 12
                    val netIncome = annualRent - (binding.utilitiesInput.text.toString().toDoubleOrNull() ?: 0.0) * 12 - (binding.capExpensesInput.text.toString().toDoubleOrNull() ?: 0.0) - annualMortgage
                    val taxOnIncome = if (netIncome > 0) netIncome * (binding.taxRateInput.text.toString().toDoubleOrNull()?.div(100) ?: 0.13) else 0.0
                    add(netIncome - taxOnIncome)
                }
            }

            cashFlows.forEachIndexed { index, value ->
                val dataRow = sheet.createRow(index + 1)
                dataRow.createCell(0).setCellValue(index.toDouble())
                dataRow.createCell(1).setCellValue(value)
            }

            val file = File(requireContext().getExternalFilesDir(null), "RealEstate_${System.currentTimeMillis()}.xlsx")
            FileOutputStream(file).use { outputStream ->
                wb.write(outputStream)
            }
            binding.resultText.text = "Excel сохранён в ${file.absolutePath}"
            binding.resultText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }

        // Экспорт в PDF
        binding.exportPdfButton.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
                return@setOnClickListener
            }

            val propertyCost = binding.propertyCostInput.text.toString().toDoubleOrNull() ?: 0.0
            val result = binding.resultText.text.toString()
            val file = File(requireContext().getExternalFilesDir(null), "RealEstate_Report_${System.currentTimeMillis()}.pdf")
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            document.add(Paragraph("Отчёт по недвижимости"))
            document.add(Paragraph("Стоимость объекта: $propertyCost руб"))
            document.add(Paragraph(result))
            document.close()
            binding.resultText.text = "PDF сохранён в ${file.absolutePath}"
            binding.resultText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}