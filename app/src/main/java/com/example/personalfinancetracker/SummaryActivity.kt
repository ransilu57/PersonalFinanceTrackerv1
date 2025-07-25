package com.example.personalfinancetracker

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetracker.databinding.ActivitySummaryBinding
import com.example.personalfinancetracker.model.CategorySummary
import com.example.personalfinancetracker.model.Transaction
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySummaryBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var settingsPreferences: SharedPreferences
    private val gson = Gson()
    private val calendar: Calendar = Calendar.getInstance()
    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable Up button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("TransactionPrefs", MODE_PRIVATE)
        settingsPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)

        // Setup date range picker
        binding.tvDateRange.setOnClickListener {
            showDateRangePicker()
        }

        // Setup add transaction button in empty state
        binding.btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        loadSummary()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadSummary()
    }

    private fun showDateRangePicker() {
        val startYear = calendar.get(Calendar.YEAR)
        val startMonth = calendar.get(Calendar.MONTH)
        val startDay = calendar.get(Calendar.DAY_OF_MONTH)

        val startDatePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                startDate = calendar.time
                showEndDatePicker()
            },
            startYear, startMonth, startDay
        )
        startDatePicker.datePicker.maxDate = System.currentTimeMillis()
        startDatePicker.show()
    }

    private fun showEndDatePicker() {
        val endYear = calendar.get(Calendar.YEAR)
        val endMonth = calendar.get(Calendar.MONTH)
        val endDay = calendar.get(Calendar.DAY_OF_MONTH)

        val endDatePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                endDate = calendar.time
                updateDateRangeText()
                loadSummary()
            },
            endYear, endMonth, endDay
        )
        endDatePicker.datePicker.maxDate = System.currentTimeMillis()
        startDate?.let { endDatePicker.datePicker.minDate = it.time }
        endDatePicker.show()
    }

    private fun updateDateRangeText() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val start = startDate?.let { sdf.format(it) } ?: "Start"
        val end = endDate?.let { sdf.format(it) } ?: "End"
        binding.tvDateRange.text = "$start - $end"
    }

    private fun loadSummary() {
        val json = sharedPreferences.getString("transactions", null)
        val transactions = if (json != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson<List<Transaction>>(json, type)
        } else {
            emptyList()
        }

        // Filter transactions by date range
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val filteredTransactions = transactions.filter { transaction ->
            val transactionDate = sdf.parse(transaction.date)
            val inRange = (startDate == null || !transactionDate.before(startDate)) &&
                    (endDate == null || !transactionDate.after(endDate))
            inRange
        }

        // Separate expenses and incomes
        val expenses = filteredTransactions.filter { it.type == "expense" }
        val incomes = filteredTransactions.filter { it.type == "income" }

        // Group expenses by category
        val expenseSummaries = expenses
            .groupBy { it.category }
            .map { (category, transactions) ->
                CategorySummary(category, transactions.sumOf { it.amount })
            }
            .sortedBy { it.category }

        // Group incomes by category
        val incomeSummaries = incomes
            .groupBy { it.category }
            .map { (category, transactions) ->
                CategorySummary(category, transactions.sumOf { it.amount })
            }
            .sortedBy { it.category }

        // Calculate totals
        val totalExpenses = expenseSummaries.sumOf { it.totalAmount }
        val totalIncomes = incomeSummaries.sumOf { it.totalAmount }
        val netBalance = totalIncomes - totalExpenses
        val currency = settingsPreferences.getString("currency", "USD") ?: "USD"

        binding.tvTotalExpenses.text = "Total Expenses: $currency${String.format("%.2f", totalExpenses)}"
        binding.tvTotalIncomes.text = "Total Incomes: $currency${String.format("%.2f", totalIncomes)}"
        binding.tvNetBalance.text = "Net Balance: $currency${String.format("%.2f", netBalance)}"

        // Setup ViewPager and Tabs
        val pagerAdapter = SummaryPagerAdapter(this, expenseSummaries, incomeSummaries, currency, totalExpenses, totalIncomes)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Expenses"
                1 -> "Incomes"
                else -> throw IllegalStateException("Invalid tab position")
            }
        }.attach()

        // Handle empty state
        val isEmpty = filteredTransactions.isEmpty()
        binding.tabLayout.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.viewPager.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
}