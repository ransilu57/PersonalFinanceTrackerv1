package com.example.personalfinancetracker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetracker.databinding.ActivitySettingsBinding
import com.example.personalfinancetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var transactionPreferences: SharedPreferences
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable Up button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        transactionPreferences = getSharedPreferences("TransactionPrefs", MODE_PRIVATE)

        // Setup currency spinner
        binding.spinnerCurrency.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.currency_options,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Load saved settings
        binding.etBudget.setText(sharedPreferences.getFloat("budget", 0f).toString())
        val currencies = resources.getStringArray(R.array.currency_options)
        binding.spinnerCurrency.setSelection(
            currencies.indexOf(sharedPreferences.getString("currency", "USD") ?: "USD")
        )

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        binding.btnExport.setOnClickListener {
            exportData()
        }

        binding.btnImport.setOnClickListener {
            importData()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun saveSettings() {
        val budget = binding.etBudget.text.toString().toFloatOrNull() ?: 0f
        val currency = binding.spinnerCurrency.selectedItem.toString()

        if (budget < 10f) {
            Toast.makeText(this, "Budget must be at least 10", Toast.LENGTH_SHORT).show()
            return
        }

        with(sharedPreferences.edit()) {
            putFloat("budget", budget)
            putString("currency", currency)
            apply()
        }
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun exportData() {
        try {
            val json = transactionPreferences.getString("transactions", null)
            if (json == null) {
                Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
                return
            }
            val file = File(filesDir, "transactions.json")
            file.writeText(json)
            Toast.makeText(this, "Data exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        try {
            val file = File(filesDir, "transactions.json")
            if (!file.exists()) {
                Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show()
                return
            }
            val json = file.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = gson.fromJson(json, type)
            with(transactionPreferences.edit()) {
                putString("transactions", json)
                apply()
            }
            Toast.makeText(this, "Data imported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}