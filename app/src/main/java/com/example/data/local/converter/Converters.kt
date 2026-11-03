package com.example.data.local.converter

import androidx.room.TypeConverter
import com.example.domain.model.PaymentMethod
import com.example.domain.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType = value?.let {
        runCatching { TransactionType.valueOf(it) }.getOrDefault(TransactionType.EXPENSE)
    } ?: TransactionType.EXPENSE

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod?): String? = method?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod = value?.let {
        runCatching { PaymentMethod.valueOf(it) }.getOrDefault(PaymentMethod.CASH)
    } ?: PaymentMethod.CASH
}
