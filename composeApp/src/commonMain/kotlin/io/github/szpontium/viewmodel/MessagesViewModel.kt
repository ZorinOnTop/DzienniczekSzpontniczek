package io.github.szpontium.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.szpontium.api.prometheus.PrometheusMessagesApi
import io.github.szpontium.session.ApiSession
import io.github.szpontium.ui.model.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class MessageTab {
    RECEIVED, SENT, DELETED
}

data class MessagesState(
    val isLoading: Boolean = false,
    val messages: List<UiMessage> = emptyList(),
    val error: String? = null,
    val currentTab: MessageTab = MessageTab.RECEIVED
)

class MessagesViewModel(
    private val session: ApiSession
) : ViewModel() {

    private val _state = MutableStateFlow(MessagesState(isLoading = true))
    val state: StateFlow<MessagesState> = _state

    init {
        loadMessages()
    }

    fun setTab(tab: MessageTab) {
        if (_state.value.currentTab != tab) {
            _state.value = _state.value.copy(currentTab = tab, isLoading = true, messages = emptyList(), error = null)
            loadMessages()
        }
    }

    fun loadMessages() {
        val account = session.currentAccount ?: return
        val api = session.api ?: return
        val currentTab = _state.value.currentTab

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Determine if we can use PrometheusMessagesApi
                val prometheusApi = session.prometheusMessagesApi
                if (prometheusApi != null && account.unit.restUrl.contains("hebe", ignoreCase = true).not()) {
                    // Initialize if needed
                    try {
                        prometheusApi.initialize()
                    } catch (e: Exception) {
                        // ignore or handle initialization error
                    }
                    val mailboxKey = session.prometheusMailboxKey ?: "" // Should be populated during login, but as fallback we pass empty or we need to extract from somewhere. 
                    // Wait, Prometheus API needs a mailbox key. Let's look at SzpontApi - it also needs `box`.
                    val box = account.messageBox?.globalKey ?: ""
                    
                    val pMessages = when (currentTab) {
                        MessageTab.RECEIVED -> prometheusApi.getReceivedMessages(mailboxKey = box)
                        MessageTab.SENT -> prometheusApi.getSentMessages(mailboxKey = box)
                        MessageTab.DELETED -> prometheusApi.getDeletedMessages(mailboxKey = box)
                    }
                    
                    val uiMessages = pMessages.map { pMsg ->
                        UiMessage(
                            id = pMsg.apiGlobalKey,
                            title = pMsg.temat,
                            senderOrRecipient = pMsg.korespondenci ?: "Nieznany",
                            date = try { kotlinx.datetime.LocalDateTime.parse(pMsg.data.removeSuffix("Z")) } catch (e: Exception) { null },
                            isUnread = !pMsg.przeczytana,
                            hasAttachments = pMsg.hasZalaczniki
                        )
                    }
                    _state.value = _state.value.copy(isLoading = false, messages = uiMessages)
                } else {
                    // Use Hebe API
                    val box = account.messageBox?.globalKey ?: ""
                    val hMessages = when (currentTab) {
                        MessageTab.RECEIVED -> api.getReceivedMessages(restUrl = account.unit.restUrl, box = box, pupilId = account.pupil.id)
                        MessageTab.SENT -> api.getSentMessages(restUrl = account.unit.restUrl, box = box, pupilId = account.pupil.id)
                        MessageTab.DELETED -> api.getDeletedMessages(restUrl = account.unit.restUrl, box = box, pupilId = account.pupil.id)
                    }
                    
                    val uiMessages = hMessages.map { hMsg ->
                        UiMessage(
                            id = hMsg.id,
                            title = hMsg.subject,
                            senderOrRecipient = if (currentTab == MessageTab.SENT) {
                                hMsg.receiver.firstOrNull()?.name ?: "Nieznany"
                            } else {
                                hMsg.sender.name
                            },
                            date = hMsg.sentAt,
                            isUnread = hMsg.status == 0,
                            hasAttachments = hMsg.attachments.isNotEmpty(),
                            content = hMsg.content
                        )
                    }
                    _state.value = _state.value.copy(isLoading = false, messages = uiMessages)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Wystąpił błąd podczas pobierania wiadomości")
            }
        }
    }
}
