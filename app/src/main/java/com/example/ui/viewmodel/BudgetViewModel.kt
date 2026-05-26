package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.local.BudgetDatabase
import com.example.data.model.CategoryBudget
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository

    init {
        val database = BudgetDatabase.getDatabase(application)
        repository = BudgetRepository(database.expenseDao())
    }

    // --- State Observables ---
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categoryBudgets: StateFlow<List<CategoryBudget>> = repository.categoryBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyBudget: StateFlow<MonthlyBudget> = repository.getMonthlyBudget()
        .map { it ?: MonthlyBudget() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlyBudget()
        )

    // --- AI Suggestions / Tips State ---
    private val _savingTips = MutableStateFlow<String>("")
    val savingTips: StateFlow<String> = _savingTips.asStateFlow()

    private val _isGeneratingTips = MutableStateFlow(false)
    val isGeneratingTips: StateFlow<Boolean> = _isGeneratingTips.asStateFlow()

    // --- AI Categorization Status ---
    private val _isCategorizing = MutableStateFlow(false)
    val isCategorizing: StateFlow<Boolean> = _isCategorizing.asStateFlow()

    // --- Sync & Cloud Backup State (Simulated Cloud Sync) ---
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // --- Notifications Toggles & Reminders State ---
    private val _dailyReminderEnabled = MutableStateFlow(true)
    val dailyReminderEnabled: StateFlow<Boolean> = _dailyReminderEnabled.asStateFlow()

    private val _budgetWarningEnabled = MutableStateFlow(true)
    val budgetWarningEnabled: StateFlow<Boolean> = _budgetWarningEnabled.asStateFlow()

    // --- Dark Mode State ---
    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null means follow system theme
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    init {
        // Pre-populate some initial budgets if table is empty
        viewModelScope.launch(Dispatchers.IO) {
            val currentMonthly = repository.getMonthlyBudgetDirect()
            if (currentMonthly == null) {
                repository.insertMonthlyBudget(MonthlyBudget(limitAmount = 15000.0))
            }

            // Put standard category limits if empty
            val categories = listOf("Yemek", "Ulaşım", "Kira/Fatura", "Eğlence", "Alışveriş", "Diğer")
            val existing = repository.categoryBudgets.first()
            if (existing.isEmpty()) {
                repository.insertCategoryBudget(CategoryBudget("Yemek", 3000.0))
                repository.insertCategoryBudget(CategoryBudget("Ulaşım", 1500.0))
                repository.insertCategoryBudget(CategoryBudget("Kira/Fatura", 6000.0))
                repository.insertCategoryBudget(CategoryBudget("Eğlence", 2000.0))
                repository.insertCategoryBudget(CategoryBudget("Alışveriş", 2000.0))
                repository.insertCategoryBudget(CategoryBudget("Diğer", 500.0))
            }
        }
    }

    // --- CRUD Actions ---
    fun addExpense(title: String, amount: Double, category: String, notes: String = "", timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = Expense(
                title = title.trim(),
                amount = amount,
                category = category,
                notes = notes.trim(),
                timestamp = timestamp
            )
            repository.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    fun deleteExpenseById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpenseById(id)
        }
    }

    fun updateMonthlyBudget(amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMonthlyBudget(MonthlyBudget(limitAmount = amount))
        }
    }

    fun updateCategoryBudget(category: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategoryBudget(CategoryBudget(category, amount))
        }
    }

    // --- Theme Control ---
    fun toggleDarkMode() {
        _isDarkMode.value = when (_isDarkMode.value) {
            null -> true
            true -> false
            false -> null
        }
    }

    // --- Smart Savings Recommendations (Gemini API) ---
    fun generateSavingsRecommendations() {
        viewModelScope.launch {
            _isGeneratingTips.value = true
            _savingTips.value = ""

            val expenses = allExpenses.value
            val totalBudget = monthlyBudget.value.limitAmount
            val totalSpent = expenses.sumOf { it.amount }

            val expenseSummary = if (expenses.isEmpty()) {
                "Henüz hiç harcama girilmedi."
            } else {
                expenses.groupBy { it.category }
                    .map { (cat, list) -> "$cat: ${list.sumOf { it.amount }} TL" }
                    .joinToString(", ")
            }

            val prompt = """
                Sen akıllı bir kişisel finans ve tasarruf danışmanısın. Kullanıcının aylık bütçe limiti $totalBudget TL'dir.
                Şu ana kadarki harcama listesi özeti şöyledir: $expenseSummary. Toplam yapılan harcama tutarı: $totalSpent TL'dir.
                Lütfen bu harcama profiline göre kullanıcının nasıl tasarruf edebileceğine dair detaylı, pratik, motive edici ve Türkçe 3 özel tasarruf önerisi sun.
                Öneriler maddeler halinde olsun, her maddenin başlığı dikkat çekici ve açıklayıcı olsun. Dil samimi, rehberlik edici ve anlaşılır olmalı.
            """.trimIndent()

            val textResult = withContext(Dispatchers.IO) {
                GeminiApiClient.getGeminiResponse(prompt)
            }
            _savingTips.value = textResult
            _isGeneratingTips.value = false
        }
    }

    // --- Automated Category Suggestion (Gemini API) ---
    fun suggestCategory(title: String, onCompleted: (String) -> Unit) {
        if (title.isBlank()) {
            onCompleted("Diğer")
            return
        }

        viewModelScope.launch {
            _isCategorizing.value = true
            val prompt = """
                Verilen harcama açıklamasını ("$title") analiz et ve aşağıdaki kategorilerden sadece birini tam olarak yaz.
                Sadece seçilen kategorinin kelimesini döndür, ek açıklama veya noktalama işaretleri ekleme. Keyfî seçim yapma, harcamanın türüne en yakın olan kategoriyi seç.
                
                Kategoriler:
                - Yemek
                - Ulaşım
                - Kira/Fatura
                - Eğlence
                - Alışveriş
                - Diğer
                
                Örnek Giriş: Starbucks kahve
                Örnek Çıkış: Yemek
                
                Örnek Giriş: Metro bileti
                Örnek Çıkış: Ulaşım
            """.trimIndent()

            val categoryResult = withContext(Dispatchers.IO) {
                val response = GeminiApiClient.getGeminiResponse(prompt).trim()
                val allowedCategories = listOf("Yemek", "Ulaşım", "Kira/Fatura", "Eğlence", "Alışveriş", "Diğer")
                // Clean response and match to allowed ones
                allowedCategories.firstOrNull { it.lowercase() == response.lowercase() } ?: "Diğer"
            }
            onCompleted(categoryResult)
            _isCategorizing.value = false
        }
    }

    // --- Daily Simulated Reminder Toggles ---
    fun toggleDailyReminder(enabled: Boolean) {
        _dailyReminderEnabled.value = enabled
    }

    fun toggleBudgetWarning(enabled: Boolean) {
        _budgetWarningEnabled.value = enabled
    }

    // --- Simulated Cloud Backup / Multi-Device Sync ---
    fun triggerCloudSync() {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            // Simulate network delay to different devices
            delay(2500)
            try {
                _lastSyncTime.value = System.currentTimeMillis()
                _syncState.value = SyncState.SUCCESS
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.IDLE
    }
}
