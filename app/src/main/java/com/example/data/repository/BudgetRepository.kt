package com.example.data.repository

import com.example.data.local.ExpenseDao
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import com.example.data.model.CategoryBudget
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    val categoryBudgets: Flow<List<CategoryBudget>> = expenseDao.getCategoryBudgetsFlow()

    fun getMonthlyBudget(monthId: String = "default_monthly_budget"): Flow<MonthlyBudget?> =
        expenseDao.getMonthlyBudgetFlow(monthId)

    suspend fun getMonthlyBudgetDirect(monthId: String = "default_monthly_budget"): MonthlyBudget? =
        expenseDao.getMonthlyBudgetDirect(monthId)

    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Int) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun insertMonthlyBudget(budget: MonthlyBudget) {
        expenseDao.insertMonthlyBudget(budget)
    }

    suspend fun insertCategoryBudget(categoryBudget: CategoryBudget) {
        expenseDao.insertCategoryBudget(categoryBudget)
    }

    suspend fun deleteCategoryBudget(category: String) {
        expenseDao.deleteCategoryBudget(category)
    }
}
