package com.example.data.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity

data class TransactionWithCategory(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
