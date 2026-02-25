package fr.triquet.manyinone.loyalty

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.triquet.manyinone.data.local.AppDatabase
import fr.triquet.manyinone.data.local.LoyaltyCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoyaltyCardsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).loyaltyCardDao()

    private val _cards = MutableStateFlow<List<LoyaltyCard>>(emptyList())
    val cards: StateFlow<List<LoyaltyCard>> = _cards

    init {
        viewModelScope.launch {
            dao.getAll().collect { _cards.value = it }
        }
    }

    suspend fun getById(id: Long): LoyaltyCard? = dao.getById(id)

    fun addCard(name: String, value: String, format: String, color: Int) {
        viewModelScope.launch {
            val nextOrder = dao.nextSortOrder()
            dao.insert(LoyaltyCard(name = name, barcodeValue = value, barcodeFormat = format, color = color, sortOrder = nextOrder))
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

    fun moveCard(fromIndex: Int, toIndex: Int) {
        val list = _cards.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _cards.value = list
    }

    fun commitCardOrder() {
        val list = _cards.value
        viewModelScope.launch {
            list.forEachIndexed { index, card ->
                if (card.sortOrder != index) {
                    dao.updateSortOrder(card.id, index)
                }
            }
        }
    }
}
