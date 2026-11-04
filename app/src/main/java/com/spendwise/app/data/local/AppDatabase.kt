package com.spendwise.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendwise.app.data.local.converter.Converters
import com.spendwise.app.data.local.dao.BudgetDao
import com.spendwise.app.data.local.dao.CategoryDao
import com.spendwise.app.data.local.dao.TransactionDao
import com.spendwise.app.data.local.entity.BudgetEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "spendwise.db"

        fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default categories directly upon database creation
                        DefaultCategories.allDefaultCategories.forEach { category ->
                            db.execSQL(
                                """
                                INSERT OR IGNORE INTO categories (id, name, icon, color, isDefault, type)
                                VALUES (${category.id}, '${category.name}', '${category.icon}', ${category.color}, ${if (category.isDefault) 1 else 0}, '${category.type.name}')
                                """.trimIndent()
                            )
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Migrate any existing 'Freelance' category to 'Pocket Money'.
                        // UPDATE OR IGNORE: categories has a UNIQUE(name, type) index, so
                        // blindly renaming 'Freelance' would crash with a constraint
                        // violation if the user already created their own 'Pocket Money'
                        // category of the same type.
                        db.execSQL(
                            "UPDATE OR IGNORE categories SET name = 'Pocket Money', icon = 'savings' WHERE name = 'Freelance'"
                        )
                    }
                })
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}
