package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryMeta(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object Categories {
    val EXPENSE_CATEGORIES = listOf(
        CategoryMeta("Grocery", Icons.Filled.ShoppingBag, Color(0xFF10B981)),
        CategoryMeta("Vegetable", Icons.Filled.Yard, Color(0xFF22C55E)),
        CategoryMeta("Egg", Icons.Filled.Egg, Color(0xFFF59E0B)),
        CategoryMeta("Meat", Icons.Filled.Restaurant, Color(0xFFEF4444)),
        CategoryMeta("Fish", Icons.Filled.SetMeal, Color(0xFF06B6D4)),
        CategoryMeta("Fuel", Icons.Filled.LocalGasStation, Color(0xFFF97316)),
        CategoryMeta("Movie", Icons.Filled.Movie, Color(0xFF8B5CF6)),
        CategoryMeta("Online Purchase", Icons.Filled.ShoppingCart, Color(0xFF3B82F6)),
        CategoryMeta("Monthly Rental", Icons.Filled.Home, Color(0xFF6366F1)),
        CategoryMeta("Dining & Food", Icons.Filled.Fastfood, Color(0xFFEC4899)),
        CategoryMeta("Bills & Utilities", Icons.Filled.ReceiptLong, Color(0xFF64748B)),
        CategoryMeta("Health & Pharmacy", Icons.Filled.MedicalServices, Color(0xFF14B8A6)),
        CategoryMeta("Travel & Transport", Icons.Filled.DirectionsCar, Color(0xFF0EA5E9)),
        CategoryMeta("EMI / Loan Payment", Icons.Filled.CreditCard, Color(0xFF7C3AED)),
        CategoryMeta("Other", Icons.Filled.MoreHoriz, Color(0xFF94A3B8))
    )

    val INCOME_CATEGORIES = listOf(
        CategoryMeta("Salary", Icons.Filled.Payments, Color(0xFF10B981)),
        CategoryMeta("Business / Freelance", Icons.Filled.AccountBalance, Color(0xFF059669)),
        CategoryMeta("Investment Returns", Icons.Filled.CreditCard, Color(0xFF3B82F6)),
        CategoryMeta("Rental Income", Icons.Filled.Home, Color(0xFF8B5CF6)),
        CategoryMeta("Gift / Cashback", Icons.Filled.ShoppingCart, Color(0xFFF59E0B)),
        CategoryMeta("Other Income", Icons.Filled.MoreHoriz, Color(0xFF64748B))
    )

    fun getCategoryMeta(name: String): CategoryMeta {
        return (EXPENSE_CATEGORIES + INCOME_CATEGORIES).firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: CategoryMeta(name, Icons.Filled.MoreHoriz, Color(0xFF64748B))
    }
}
