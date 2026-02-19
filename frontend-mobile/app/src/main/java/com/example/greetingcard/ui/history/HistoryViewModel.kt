package com.example.greetingcard.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greetingcard.data.api.ReadingDto
import com.example.greetingcard.data.readings.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReadingsState {
    object Loading : ReadingsState()
    data class Success(val readings: List<ReadingDto>) : ReadingsState()
    data class Error(val message: String) : ReadingsState()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val readingRepository: ReadingRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ReadingsState>(ReadingsState.Loading)
    val state: StateFlow<ReadingsState> = _state.asStateFlow()

    init {
        loadReadings()
    }

    fun loadReadings() {
        viewModelScope.launch {
            _state.value = ReadingsState.Loading
            val result = readingRepository.getReadings()
            _state.value = if (result.isSuccess) {
                ReadingsState.Success(
                    result.getOrNull()!!.sortedByDescending { it.timestamp }
                )
            } else {
                ReadingsState.Error(result.exceptionOrNull()?.message ?: "Failed to load readings")
            }
        }
    }
}
