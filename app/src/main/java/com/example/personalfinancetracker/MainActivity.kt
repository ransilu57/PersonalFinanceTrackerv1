package com.example.personalfinancetracker

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.personalfinancetracker.adapter.TransactionAdapter
import com.example.personalfinancetracker.databinding.ActivityMainBinding
import com.example.personalfinancetracker.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit
import android.app.NotificationChannel
import android.app.NotificationManager
import android.view.View
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var settingsPreferences: SharedPreferences
    private lateinit var adapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()
    private val gson = Gson()
    private val CHANNEL_ID = "budget_notifications"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }

        sharedPreferences = getSharedPreferences("TransactionPrefs", MODE_PRIVATE)
        settingsPreferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        setupRecyclerView()
        loadTransactions()
        requestNotificationPermission()
        scheduleDailyReminder()

        // Update click listeners to use the new LinearLayout IDs
        binding.btnAddTransactionWrapper.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        binding.btnViewSummaryWrapper.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        binding.btnSettingsWrapper.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        val currency = settingsPreferences.getString("currency", "USD") ?: "USD"
        adapter = TransactionAdapter(
            transactions.toList(),
            currency,
            onItemClick = { transaction ->
                val intent = Intent(this, AddTransactionActivity::class.java)
                intent.putExtra("transaction", transaction)
                startActivity(intent)
            },
            onItemLongClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        binding.rvTransactions.adapter = adapter
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        Log.d("MainActivity", "RecyclerView setup with adapter: $adapter")
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete '${transaction.title}'?")
            .setPositiveButton("Yes") { _, _ ->
                deleteTransaction(transaction)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        val json = sharedPreferences.getString("transactions", null)
        val transactions = if (json != null) {
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            gson.fromJson<MutableList<Transaction>>(json, type)
        } else {
            mutableListOf()
        }

        transactions.removeAll { it.id == transaction.id }
        with(sharedPreferences.edit()) {
            putString("transactions", gson.toJson(transactions))
            apply()
        }

        loadTransactions()
        Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
    }

    private fun loadTransactions() {
        Log.d("MainActivity", "loadTransactions called")
        val json = sharedPreferences.getString("transactions", null)
        Log.d("MainActivity", "Loaded JSON: $json")
        if (json != null && json.isNotEmpty()) {
            try {
                val type = object : TypeToken<List<Transaction>>() {}.type
                val savedTransactions: List<Transaction> = gson.fromJson(json, type) ?: emptyList()
                Log.d("MainActivity", "Loaded transactions: $savedTransactions")
                transactions.clear()
                transactions.addAll(savedTransactions)
                Log.d("MainActivity", "Calling updateTransactions with: $transactions")
                adapter.updateTransactions(transactions.toList())
                binding.rvTransactions.invalidate()
                binding.rvTransactions.requestLayout()
                checkBudget()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error parsing transactions: ${e.message}", e)
                transactions.clear()
                adapter.updateTransactions(emptyList())
                binding.rvTransactions.invalidate()
                binding.rvTransactions.requestLayout()
                binding.budgetProgressLabel.text = "Error loading transactions"
                binding.budgetProgress.progress = 0
            }
        } else {
            Log.d("MainActivity", "No transactions found in SharedPreferences")
            transactions.clear()
            adapter.updateTransactions(emptyList())
            binding.rvTransactions.invalidate()
            binding.rvTransactions.requestLayout()
            binding.budgetProgressLabel.text = "No transactions yet"
            binding.budgetProgress.progress = 0
        }
        Log.d("MainActivity", "RecyclerView visibility: ${binding.rvTransactions.visibility}")
        binding.rvTransactions.visibility = View.VISIBLE
    }

    private fun checkBudget() {
        val budget = settingsPreferences.getFloat("budget", 0f).toDouble()
        Log.d("MainActivity", "Checking budget. Budget: $budget, Transactions: $transactions")
        if (budget == 0.0) {
            binding.budgetProgressLabel.text = "Set a budget in Settings"
            binding.budgetProgress.progress = 0
            return
        }

        val totalSpent = transactions.sumOf { it.amount }
        Log.d("MainActivity", "Total spent: $totalSpent")
        val percentage = ((totalSpent / budget) * 100).coerceAtMost(100.0).toInt()
        val currency = settingsPreferences.getString("currency", "USD") ?: "USD"

        binding.budgetProgressLabel.text = "Budget: ${currency}${String.format("%.2f", budget)} | Spent: ${currency}${String.format("%.2f", totalSpent)}"
        binding.budgetProgress.progress = percentage

        if (percentage >= 100) {
            showNotification("Budget Exceeded", "You have exceeded your budget of ${currency}${String.format("%.2f", budget)}!")
        } else if (percentage >= 80) {
            showNotification("Budget Warning", "You are approaching your budget limit!")
        }
    }

    private fun showNotification(title: String, message: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkBudget()
        }
    }

    private fun scheduleDailyReminder() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
        loadTransactions()
    }
}