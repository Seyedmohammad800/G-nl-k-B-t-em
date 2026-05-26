package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String, // e.g., "Yemek", "Ulaşım", "Kira/Fatura", "Eğlence", "Alışveriş", "Diğer"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
