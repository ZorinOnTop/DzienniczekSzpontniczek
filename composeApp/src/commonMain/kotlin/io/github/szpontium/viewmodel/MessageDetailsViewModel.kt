package io.github.szpontium.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.szpontium.session.ApiSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MessageDetailsState(
    val isLoading: Boolean = false,
    val content: String? = null,
    val sender: String? = null,
    val subject: String? = null,
    val date: String? = null,
    val error: String? = null
)

class MessageDetailsViewModel(
    private val session: ApiSession
) : ViewModel() {

    private val _state = MutableStateFlow(MessageDetailsState())
    val state: StateFlow<MessageDetailsState> = _state

    private var loadedId: String? = null

    fun loadMessage(id: String, isHebe: Boolean, hebeContent: String?) {
        if (loadedId == id) return
        loadedId = id

        viewModelScope.launch {
            if (isHebe) {
                // Hebe already provides the content from the list
                _state.value = _state.value.copy(
                    isLoading = false,
                    content = hebeContent ?: "Brak treści wiadomości."
                )
            } else {
                _state.value = _state.value.copy(isLoading = true, error = null)
                try {
                    val prometheusApi = session.prometheusMessagesApi
                        ?: throw IllegalStateException("Prometheus API not initialized")
                    
                    val details = prometheusApi.getMessageDetails(apiGlobalKey = id)
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        content = details.tresc,
                        sender = details.nadawca,
                        subject = details.temat,
                        date = details.data
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Wystąpił błąd podczas pobierania wiadomości"
                    )
                }
            }
        }
    }
}
