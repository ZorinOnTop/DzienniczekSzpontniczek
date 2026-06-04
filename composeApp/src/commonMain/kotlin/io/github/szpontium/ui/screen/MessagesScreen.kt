package io.github.szpontium.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.szpontium.ui.model.UiMessage
import io.github.szpontium.viewmodel.MessageTab
import io.github.szpontium.viewmodel.MessagesViewModel
import org.koin.compose.viewmodel.koinViewModel

import io.github.szpontium.navigation.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(onNavigate: (Route) -> Unit, viewModel: MessagesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val tabs = listOf(
        MessageTab.RECEIVED to "Odebrane",
        MessageTab.SENT to "Wysłane",
        MessageTab.DELETED to "Usunięte"
    )
    val selectedTabIndex = tabs.indexOfFirst { it.first == state.currentTab }.takeIf { it >= 0 } ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, (tab, title) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { viewModel.setTab(tab) },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    ErrorScreen(state.error!!, onRetry = { viewModel.loadMessages() })
                }
                state.messages.isEmpty() -> {
                    EmptyScreen("Brak wiadomości w tym folderze")
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.messages) { message ->
                            MessageCard(message, onClick = {
                                onNavigate(Route.MessageDetails(id = message.id, isHebe = message.content != null, hebeContent = message.content))
                            })
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: UiMessage, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.title.ifBlank { "(brak tematu)" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (message.isUnread) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = message.senderOrRecipient,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (message.isUnread) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (message.hasAttachments) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = "Złącznik",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (message.date != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${message.date.dayOfMonth}.${message.date.monthNumber}.${message.date.year}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
