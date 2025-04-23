package com.example.personalfinancetracker

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.personalfinancetracker.model.CategorySummary

class SummaryPagerAdapter(
    fragment: SummaryActivity,
    private val expenseSummaries: List<CategorySummary>,
    private val incomeSummaries: List<CategorySummary>,
    private val currency: String,
    private val totalExpenses: Double,
    private val totalIncomes: Double
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2 // Two tabs: Expenses and Incomes

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SummaryTabFragment.newInstance(expenseSummaries, currency, totalExpenses)
            1 -> SummaryTabFragment.newInstance(incomeSummaries, currency, totalIncomes)
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}