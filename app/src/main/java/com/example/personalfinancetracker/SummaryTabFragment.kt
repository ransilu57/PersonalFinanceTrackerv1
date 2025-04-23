package com.example.personalfinancetracker

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.adapter.SummaryAdapter
import com.example.personalfinancetracker.databinding.FragmentSummaryTabBinding
import com.example.personalfinancetracker.model.CategorySummary
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class SummaryTabFragment : Fragment() {

    private var _binding: FragmentSummaryTabBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_SUMMARIES = "summaries"
        private const val ARG_CURRENCY = "currency"
        private const val ARG_TOTAL = "total"

        fun newInstance(summaries: List<CategorySummary>, currency: String, total: Double): SummaryTabFragment {
            val fragment = SummaryTabFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_SUMMARIES, ArrayList(summaries))
            args.putString(ARG_CURRENCY, currency)
            args.putDouble(ARG_TOTAL, total)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve arguments
        val summaries: List<CategorySummary> = arguments?.getParcelableArrayList<CategorySummary>(ARG_SUMMARIES) ?: emptyList()
        val currency = arguments?.getString(ARG_CURRENCY) ?: "USD"
        val total = arguments?.getDouble(ARG_TOTAL) ?: 0.0

        // Setup Pie Chart
        setupPieChart(binding.pieChart)

        // Setup RecyclerView
        binding.rvSummary.adapter = SummaryAdapter(summaries, currency, total)
        binding.rvSummary.layoutManager = LinearLayoutManager(context)

        // Populate Pie Chart
        if (summaries.isNotEmpty()) {
            val entries = summaries.map { PieEntry(it.totalAmount.toFloat(), it.category) }
            val dataSet = PieDataSet(entries, "By Category")
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.BLACK
            dataSet.valueFormatter = PercentFormatter(binding.pieChart)

            val pieData = PieData(dataSet)
            binding.pieChart.data = pieData
            binding.pieChart.invalidate()
            binding.pieChart.visibility = View.VISIBLE
            binding.pieChart.animateY(1000)
        } else {
            binding.pieChart.visibility = View.GONE
        }

        // Handle empty state for RecyclerView
        binding.rvSummary.visibility = if (summaries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupPieChart(pieChart: PieChart) {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(false)
        pieChart.setDrawHoleEnabled(true)
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleRadius(40f)
        pieChart.setHoleRadius(35f)
        pieChart.legend.isEnabled = true
        pieChart.legend.textColor = Color.DKGRAY
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(12f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}