package fr.triquet.manyinone.loyalty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.triquet.manyinone.data.local.AppDatabase
import fr.triquet.manyinone.data.local.LoyaltyCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoyaltyCardsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).loyaltyCardDao()

    val cards: StateFlow<List<LoyaltyCard>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getById(id: Long): LoyaltyCard? = dao.getById(id)

    fun addCard(name: String, value: String, format: String, color: Int) {
        viewModelScope.launch {
            dao.insert(LoyaltyCard(name = name, barcodeValue = value, barcodeFormat = format, color = color))
        }
    }

    fun updateCard(card: LoyaltyCard) {
        viewModelScope.launch {
            dao.update(card)
        }
    }

    fun deleteCard(card: LoyaltyCard) {
        viewModelScope.launch {
            dao.delete(card)
        }
    }
}
