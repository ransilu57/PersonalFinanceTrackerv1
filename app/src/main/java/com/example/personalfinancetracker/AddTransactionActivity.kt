package com.example.personalfinancetracker

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinancetracker.databinding.ActivityAddTransactionBinding
import com.example.personalfinancetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var settingsPreferences: SharedPreferences
    private val gson = Gson()
    private var transactionId: String? = null
    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable Up button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("TransactionPrefs", MODE_PRIVATE)
        settingsPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)

        // Set currency hint for amount
        val currency = settingsPreferences.getString("currency", "USD") ?: "USD"
        binding.etAmount.hint = "Amount ($currency)"

        // Setup transaction type spinner
        binding.spinnerType.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Setup category spinner
        binding.spinnerCategory.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.transaction_categories,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Setup date picker
        binding.tvDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    binding.tvDate.text = selectedDate
                },
                year, month, day
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        // Check if editing a transaction
        intent.getParcelableExtra<Transaction>("transaction")?.let { transaction ->
            transactionId = transaction.id
            binding.etTitle.setText(transaction.title)
            binding.etAmount.setText(transaction.amount.toString())
            binding.tvDate.text = transaction.date
            val categories = resources.getStringArray(R.array.transaction_categories)
            binding.spinnerCategory.setSelection(categories.indexOf(transaction.category))
            val types = resources.getStringArray(R.array.transaction_types)
            binding.spinnerType.setSelection(
                if (transaction.type == "income") 1 else 0
            )
            binding.btnSaveTransaction.text = "Update"
        }

        binding.btnSaveTransaction.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
                finish()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun validateInputs(): Boolean {
        val title = binding.etTitle.text.toString()
        val amount = binding.etAmount.text.toString()
        val date = binding.tvDate.text.toString()

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return false
        }
        if (date.isEmpty() || !isValidDate(date)) {
            Toast.makeText(this, "Select a valid date (YYYY-MM-DD) not in the future", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsedDate = sdf.parse(date)
            if (parsedDate == null || parsedDate.after(Date())) {
                return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString()
        val amount = binding.etAmount.text.toString().toDouble()
        val category = binding.spinnerCategory.selectedItem.toString()
        val date = binding.tvDate.text.toString()
        val type = if (binding.spinnerType.selectedItem.toString() == "Income") "income" else "expense"

        val transaction = Transaction(
            id = transactionId ?: UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            category = category,
            date = date,
            type = type
        )

        // Load existing transactions
        val json = sharedPreferences.getString("transactions", null)
        val transactions = if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            gson.fromJson<MutableList<Transaction>>(json, type)
        } else {
            mutableListOf()
        }

        // Update or add transaction
        val existingIndex = transactions.indexOfFirst { it.id == transaction.id }
        if (existingIndex != -1) {
            transactions[existingIndex] = transaction
            Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
        } else {
            transactions.add(transaction)
            Toast.makeText(this, "Transaction added", Toast.LENGTH_SHORT).show()
        }

        // Save to SharedPreferences
        with(sharedPreferences.edit()) {
            putString("transactions", gson.toJson(transactions))
            apply()
        }

        Log.d("AddTransactionActivity", "Saved transactions: ${gson.toJson(transactions)}")
    }
}