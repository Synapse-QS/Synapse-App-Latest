package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.InboxViewModel
import com.synapse.social.studioasinc.domain.model.ChatSwipeGesture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String?, String?, String?) -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val archivedConversations by viewModel.archivedConversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val chatListLayout by viewModel.chatListLayout.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archived Chats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ChatsTabScreen(
            conversations = archivedConversations,
            isLoading = isLoading,
            error = error,
            onConversationClick = onNavigateToChat,
            onRetry = { viewModel.loadConversations() },
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            chatListLayout = chatListLayout,
            chatSwipeGesture = ChatSwipeGesture.UNARCHIVE,
            onSwipeAction = { chatId, gesture ->
                if (gesture == ChatSwipeGesture.DELETE) {
                    viewModel.deleteChat(chatId, false)
                } else {
                    viewModel.unarchiveChat(chatId)
                }
            }
        )
    }
}
