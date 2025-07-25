package com.example.personalfinancetracker.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.databinding.ItemTransactionBinding
import com.example.personalfinancetracker.model.Transaction

class TransactionAdapter(
    initialTransactions: List<Transaction>,
    private val currency: String,
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val transactions: MutableList<Transaction> = initialTransactions.toMutableList()

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            Log.d("TransactionAdapter", "Binding transaction at position $adapterPosition: $transaction")
            binding.tvTitle.text = transaction.title
            // Display amount with sign based on type
            val amountSign = if (transaction.type == "income") "+" else "-"
            binding.tvAmount.text = "$amountSign$currency${String.format("%.2f", transaction.amount)}"
            binding.tvAmount.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (transaction.type == "income") R.color.income_color else R.color.expense_color
                )
            )
            binding.tvCategory.text = transaction.category
            binding.tvDate.text = transaction.date
            // Display transaction type
            binding.tvType.text = if (transaction.type == "income") "Income" else "Expense"
            binding.tvType.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (transaction.type == "income") R.color.income_color else R.color.expense_color
                )
            )
            binding.root.setOnClickListener { onItemClick(transaction) }
            binding.root.setOnLongClickListener {
                onItemLongClick(transaction)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int {
        Log.d("TransactionAdapter", "Item count: ${transactions.size}")
        return transactions.size
    }

    fun updateTransactions(newTransactions: List<Transaction>) {
        Log.d("TransactionAdapter", "Updating transactions. Old: $transactions, New: $newTransactions")
        val diffCallback = TransactionDiffCallback(transactions, newTransactions)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        transactions.clear()
        transactions.addAll(newTransactions)
        diffResult.dispatchUpdatesTo(this)
        Log.d("TransactionAdapter", "Updated transactions: $transactions")
    }

    private class TransactionDiffCallback(
        private val oldList: List<Transaction>,
        private val newList: List<Transaction>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}