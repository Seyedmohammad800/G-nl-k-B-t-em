package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey val id: String = "default_monthly_budget", // e.g. "2026-05" or just a single active budget row for simplification
    val limitAmount: Double = 15000.0 // Default budget limit
)

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey val category: String, // e.g. "Yemek", "Ulaşım"
    val limitAmount: Double
)
