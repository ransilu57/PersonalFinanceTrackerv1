package com.example.personalfinancetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalfinancetracker.R
import com.example.personalfinancetracker.model.CategorySummary

class SummaryAdapter(
    private val summaries: List<CategorySummary>,
    private val currency: String,
    private val totalExpenses: Double
) : RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {

    class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.tv_category)
        val amountTextView: TextView = itemView.findViewById(R.id.tv_amount)
        val percentageTextView: TextView = itemView.findViewById(R.id.tv_percentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val summary = summaries[position]
        holder.categoryTextView.text = summary.category
        holder.amountTextView.text = "${currency}${String.format("%.2f", summary.totalAmount)}"

        // Calculate and display percentage
        val percentage = if (totalExpenses > 0) (summary.totalAmount / totalExpenses * 100) else 0.0
        holder.percentageTextView.text = String.format("%.1f%%", percentage)
    }

    override fun getItemCount(): Int = summaries.size
}