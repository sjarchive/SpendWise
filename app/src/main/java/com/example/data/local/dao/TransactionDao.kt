package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.relations.TransactionWithCategory
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun getTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionWithCategoryById(id: Long): Flow<TransactionWithCategory?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Transaction
    @Query("SELECT * FROM transactions WHERE dateTime BETWEEN :startDate AND :endDate ORDER BY dateTime DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY dateTime DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY dateTime DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionWithCategory>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND dateTime BETWEEN :startDate AND :endDate")
    fun getTotalExpenseBetween(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND dateTime BETWEEN :startDate AND :endDate")
    fun getTotalIncomeBetween(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    /**
     * Moves all transactions from one category to another (used before deleting
     * a category so its history is preserved under a same-type default).
     * Also updates the denormalized [TransactionEntity.category] name column.
     */
    @Query(
        """
        UPDATE transactions
        SET categoryId = :newCategoryId, category = :newCategoryName
        WHERE categoryId = :oldCategoryId
        """
    )
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long, newCategoryName: String)
}
