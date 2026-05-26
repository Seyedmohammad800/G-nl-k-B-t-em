package com.example.data.local

import androidx.room.*
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import com.example.data.model.CategoryBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    // --- Monthly Budget ---
    @Query("SELECT * FROM monthly_budgets WHERE id = :monthId LIMIT 1")
    fun getMonthlyBudgetFlow(monthId: String = "default_monthly_budget"): Flow<MonthlyBudget?>

    @Query("SELECT * FROM monthly_budgets WHERE id = :monthId LIMIT 1")
    suspend fun getMonthlyBudgetDirect(monthId: String = "default_monthly_budget"): MonthlyBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBudget(budget: MonthlyBudget)

    // --- Category Budget ---
    @Query("SELECT * FROM category_budgets")
    fun getCategoryBudgetsFlow(): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(categoryBudget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE category = :category")
    suspend fun deleteCategoryBudget(category: String)
}
