package com.example.personalfinancetracker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val type: String = "expense" // "expense" or "income"
) : Parcelable