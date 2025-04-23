package com.example.personalfinancetracker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CategorySummary(
    val category: String,
    val totalAmount: Double
) : Parcelable