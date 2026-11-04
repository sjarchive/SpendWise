package com.spendwise.app.data.local

import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.domain.model.TransactionType

object DefaultCategories {
    val expenseCategories = listOf(
        CategoryEntity(
            id = 1,
            name = "Food",
            icon = "restaurant",
            color = 0xFFF97316L, // Orange
            isDefault = true,
            type = TransactionType.EXPENSE
        ),
        CategoryEntity(
            id = 2,
            name = "Transport",
            icon = "directions_car",
            color = 0xFF3B82F6L, // Blue
            isDefault = true,
            type = TransactionType.EXPENSE
        ),
        CategoryEntity(
            id = 3,
            name = "Shopping",
            icon = "shopping_bag",
            color = 0xFFEC4899L, // Pink
            isDefault = true,
            type = TransactionType.EXPENSE
        ),
        CategoryEntity(
            id = 4,
            name = "Bills",
            icon = "receipt_long",
            color = 0xFFF59E0BL, // Amber / Yellow
            isDefault = true,
            type = TransactionType.EXPENSE
        ),
        CategoryEntity(
            id = 5,
            name = "Health",
            icon = "medical_services",
            color = 0xFF10B981L, // Emerald / Green
            isDefault = true,
            type = TransactionType.EXPENSE
        ),
        CategoryEntity(
            id = 6,
            name = "Education",
            icon = "school",
            color = 0xFF8B5CF6L, // Purple
            isDefault = true,
            type = TransactionType.EXPENSE
        ),
        CategoryEntity(
            id = 7,
            name = "Entertainment",
            icon = "movie",
            color = 0xFF06B6D4L, // Cyan
            isDefault = true,
            type = TransactionType.EXPENSE
        ),
        CategoryEntity(
            id = 8,
            name = "Other",
            icon = "category",
            color = 0xFF64748BL, // Slate Grey
            isDefault = true,
            type = TransactionType.EXPENSE
        )
    )

    val incomeCategories = listOf(
        CategoryEntity(
            id = 9,
            name = "Salary",
            icon = "payments",
            color = 0xFF22C55EL, // Green
            isDefault = true,
            type = TransactionType.INCOME
        ),
        CategoryEntity(
            id = 10,
            name = "Pocket Money",
            icon = "savings",
            color = 0xFF14B8A6L, // Teal
            isDefault = true,
            type = TransactionType.INCOME
        ),
        CategoryEntity(
            id = 11,
            name = "Investment",
            icon = "trending_up",
            color = 0xFF6366F1L, // Indigo
            isDefault = true,
            type = TransactionType.INCOME
        ),
        CategoryEntity(
            id = 12,
            name = "Other Income",
            icon = "account_balance_wallet",
            color = 0xFF84CC16L, // Lime
            isDefault = true,
            type = TransactionType.INCOME
        )
    )

    val allDefaultCategories = expenseCategories + incomeCategories
}
