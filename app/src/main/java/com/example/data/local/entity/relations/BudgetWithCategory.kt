package com.example.data.local.entity.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity

data class BudgetWithCategory(
    @Embedded
    val budget: BudgetEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
