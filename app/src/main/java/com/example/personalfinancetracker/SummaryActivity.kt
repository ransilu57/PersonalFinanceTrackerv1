package com.example.personalfinancetracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalfinancetracker.adapter.SummaryAdapter
import com.example.personalfinancetracker.databinding.ActivitySummaryBinding
import com.example.personalfinancetracker.model.CategorySummary
import com.example.personalfinancetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SummaryActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySummaryBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var settingsPreferences: SharedPreferences
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable Up button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("TransactionPrefs", MODE_PRIVATE)
        settingsPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        loadSummary()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadSummary() {
        val json = sharedPreferences.getString("transactions", null)
        val transactions = if (json != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson<List<Transaction>>(json, type)
        } else {
            emptyList()
        }

        // Group by category and sum amounts
        val summaries = transactions
            .groupBy { it.category }
            .map { (category, transactions) ->
                CategorySummary(category, transactions.sumOf { it.amount })
            }
            .sortedBy { it.category }

        // Setup RecyclerView
        val currency = settingsPreferences.getString("currency", "USD") ?: "USD"
        binding.rvExpenses.adapter = SummaryAdapter(summaries, currency)
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)

        // Handle empty state
        binding.tvEmptySummary.visibility = if (summaries.isEmpty()) View.VISIBLE else View.GONE
        binding.rvExpenses.visibility = if (summaries.isEmpty()) View.GONE else View.VISIBLE
    }
}