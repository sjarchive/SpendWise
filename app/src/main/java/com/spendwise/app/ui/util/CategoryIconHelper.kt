package com.spendwise.app.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.spendwise.app.domain.model.PaymentMethod

@Immutable
data class CategoryIconOption(
    val key: String,
    val name: String,
    val icon: ImageVector
)

object CategoryIconHelper {

    val availableIcons: List<CategoryIconOption> = listOf(
        CategoryIconOption("restaurant", "Food", Icons.Default.Restaurant),
        CategoryIconOption("local_cafe", "Cafe & Snacks", Icons.Default.LocalCafe),
        CategoryIconOption("shopping_bag", "Shopping", Icons.Default.ShoppingBag),
        CategoryIconOption("directions_car", "Transport", Icons.Default.DirectionsCar),
        CategoryIconOption("receipt_long", "Bills & Utilities", Icons.Default.ReceiptLong),
        CategoryIconOption("medical_services", "Health & Medical", Icons.Default.MedicalServices),
        CategoryIconOption("school", "Education", Icons.Default.School),
        CategoryIconOption("movie", "Entertainment", Icons.Default.Movie),
        CategoryIconOption("home", "Housing & Rent", Icons.Default.Home),
        CategoryIconOption("fitness_center", "Fitness & Gym", Icons.Default.FitnessCenter),
        CategoryIconOption("flight", "Travel & Vacation", Icons.Default.Flight),
        CategoryIconOption("card_giftcard", "Gifts & Donations", Icons.Default.CardGiftcard),
        CategoryIconOption("pets", "Pets", Icons.Default.Pets),
        CategoryIconOption("sports_esports", "Gaming", Icons.Default.SportsEsports),
        CategoryIconOption("computer", "Electronics & Tech", Icons.Default.Computer),
        CategoryIconOption("payments", "Salary", Icons.Default.Payments),
        CategoryIconOption("savings", "Pocket Money", Icons.Default.Savings),
        CategoryIconOption("work", "Work & Side Gig", Icons.Default.Work),
        CategoryIconOption("trending_up", "Investments", Icons.Default.TrendingUp),
        CategoryIconOption("account_balance_wallet", "Savings & Wallet", Icons.Default.AccountBalanceWallet),
        CategoryIconOption("category", "General", Icons.Default.Category)
    )

    fun getIcon(iconKey: String?): ImageVector {
        return when (iconKey?.lowercase()) {
            "restaurant", "food" -> Icons.Default.Restaurant
            "local_cafe", "cafe" -> Icons.Default.LocalCafe
            "directions_car", "transport" -> Icons.Default.DirectionsCar
            "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
            "receipt_long", "bills" -> Icons.Default.ReceiptLong
            "medical_services", "health" -> Icons.Default.MedicalServices
            "school", "education" -> Icons.Default.School
            "movie", "entertainment" -> Icons.Default.Movie
            "home", "rent", "housing" -> Icons.Default.Home
            "fitness_center", "gym", "fitness" -> Icons.Default.FitnessCenter
            "flight", "travel" -> Icons.Default.Flight
            "card_giftcard", "gifts" -> Icons.Default.CardGiftcard
            "pets", "pet" -> Icons.Default.Pets
            "sports_esports", "gaming" -> Icons.Default.SportsEsports
            "computer", "tech", "electronics" -> Icons.Default.Computer
            "payments", "salary" -> Icons.Default.Payments
            "savings", "pocket_money", "pocket money" -> Icons.Default.Savings
            "work", "freelance" -> Icons.Default.Work
            "trending_up", "investment" -> Icons.Default.TrendingUp
            "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            else -> Icons.Default.Category
        }
    }

    val availableColors: List<Long> = listOf(
        0xFFF97316L, // Vibrant Orange
        0xFFEF4444L, // Bright Red
        0xFFEC4899L, // Rose Pink
        0xFF8B5CF6L, // Vivid Purple
        0xFF6366F1L, // Deep Indigo
        0xFF3B82F6L, // Ocean Blue
        0xFF06B6D4L, // Neon Cyan
        0xFF14B8A6L, // Rich Teal
        0xFF10B981L, // Emerald
        0xFF22C55EL, // Fresh Green
        0xFF84CC16L, // Lime
        0xFFEAB308L, // Golden Yellow
        0xFFF59E0BL, // Warm Amber
        0xFF78716CL, // Stone Warm Gray
        0xFF64748BL  // Slate
    )

    fun getPaymentMethodIcon(method: PaymentMethod): ImageVector {
        return when (method) {
            PaymentMethod.CASH -> Icons.Default.LocalAtm
            PaymentMethod.UPI -> Icons.Default.QrCode2
            PaymentMethod.CARD -> Icons.Default.CreditCard
            PaymentMethod.BANK -> Icons.Default.AccountBalance
        }
    }
}
