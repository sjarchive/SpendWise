package com.spendwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.spendwise.app.data.local.entity.BudgetEntity
import com.spendwise.app.data.local.entity.relations.BudgetWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Transaction
    @Query("SELECT * FROM budgets")
    fun getBudgetsWithCategory(): Flow<List<BudgetWithCategory>>

    // ORDER BY id makes the LIMIT 1 deterministic — without it SQLite may return
    // any matching row, which made upserts target an unpredictable budget when
    // duplicate rows existed.
    @Query("SELECT * FROM budgets WHERE categoryId IS NULL ORDER BY id LIMIT 1")
    fun getOverallBudget(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId ORDER BY id LIMIT 1")
    fun getBudgetForCategory(categoryId: Long): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)
}
