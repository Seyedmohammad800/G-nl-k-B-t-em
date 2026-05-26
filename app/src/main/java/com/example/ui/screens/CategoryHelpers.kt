package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

object CategoryHelpers {
    
    val categories = listOf("Yemek", "Ulaşım", "Kira/Fatura", "Eğlence", "Alışveriş", "Diğer")

    fun getCategoryIcon(category: String): ImageVector {
        return when (category) {
            "Yemek" -> Icons.Default.Restaurant
            "Ulaşım" -> Icons.Default.DirectionsCar
            "Kira/Fatura" -> Icons.Default.ReceiptLong
            "Eğlence" -> Icons.Default.SportsEsports
            "Alışveriş" -> Icons.Default.LocalMall
            else -> Icons.Default.Category
        }
    }

    fun getCategoryColor(category: String): Color {
        return when (category) {
            "Yemek" -> CatYemek
            "Ulaşım" -> CatUlasim
            "Kira/Fatura" -> CatKiraFatura
            "Eğlence" -> CatEglence
            "Alışveriş" -> CatAlisveris
            else -> CatDiger
        }
    }
}
