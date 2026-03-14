package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToInboxUpdatesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.InitializeE2EUseCase
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.domain.model.ChatListLayout
import com.synapse.social.studioasinc.domain.model.ChatSwipeGesture

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val subscribeToInboxUpdatesUseCase: SubscribeToInboxUpdatesUseCase,
    private val initializeE2EUseCase: InitializeE2EUseCase,

    private val archiveChatUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.ArchiveChatUseCase,
    private val unarchiveChatUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.UnarchiveChatUseCase,
    private val deleteChatUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteChatUseCase,
    private val getArchivedConversationsUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.GetArchivedConversationsUseCase,
    private val chatLockManager: com.synapse.social.studioasinc.core.util.ChatLockManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()


    private val _conversations = MutableStateFlow<List<com.synapse.social.studioasinc.shared.domain.model.chat.Conversation>>(emptyList())
    val conversations: StateFlow<List<com.synapse.social.studioasinc.shared.domain.model.chat.Conversation>> = _conversations.asStateFlow()

    private val _archivedConversations = MutableStateFlow<List<com.synapse.social.studioasinc.shared.domain.model.chat.Conversation>>(emptyList())
    val archivedConversations: StateFlow<List<com.synapse.social.studioasinc.shared.domain.model.chat.Conversation>> = _archivedConversations.asStateFlow()

    val archivedCount: StateFlow<Int> = _archivedConversations.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val chatListLayout: StateFlow<ChatListLayout> = settingsRepository.chatListLayout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatListLayout.DOUBLE_LINE
        )

    val chatSwipeGesture: StateFlow<ChatSwipeGesture> = settingsRepository.chatSwipeGesture
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatSwipeGesture.ARCHIVE
        )

    init {
        viewModelScope.launch {
            initializeE2EUseCase().onFailure { e ->
                _error.value = "Failed to initialize E2EE: ${e.message}"
            }
            loadCurrentUserProfile()
            loadConversations()
            loadArchivedConversations()
            subscribeToInboxUpdates()
        }
    }


    private suspend fun loadArchivedConversations() {
        getArchivedConversationsUseCase().onSuccess { chats ->
            _archivedConversations.value = chats
        }.onFailure { e ->
            _error.value = "Failed to load archived conversations: ${e.message}"
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            val chatToArchive = _conversations.value.find { it.chatId == chatId }
            if (chatToArchive != null) {
                // Optimistic UI update
                _conversations.value = _conversations.value.filter { it.chatId != chatId }
                _archivedConversations.value = listOf(chatToArchive) + _archivedConversations.value

                archiveChatUseCase(chatId).onFailure { e ->
                    // Revert on error
                    _conversations.value = listOf(chatToArchive) + _conversations.value
                    _archivedConversations.value = _archivedConversations.value.filter { it.chatId != chatId }
                    _error.value = "Failed to archive chat: ${e.message}"
                }
            }
        }
    }

    fun unarchiveChat(chatId: String) {
        viewModelScope.launch {
            val chatToUnarchive = _archivedConversations.value.find { it.chatId == chatId }
            if (chatToUnarchive != null) {
                // Optimistic UI update
                _archivedConversations.value = _archivedConversations.value.filter { it.chatId != chatId }
                _conversations.value = (listOf(chatToUnarchive) + _conversations.value).sortedByDescending { it.lastMessageTime }

                unarchiveChatUseCase(chatId).onFailure { e ->
                    // Revert on error
                    _archivedConversations.value = listOf(chatToUnarchive) + _archivedConversations.value
                    _conversations.value = _conversations.value.filter { it.chatId != chatId }
                    _error.value = "Failed to unarchive chat: ${e.message}"
                }
            }
        }
    }

    fun deleteChat(chatId: String, forEveryone: Boolean) {
        viewModelScope.launch {
            val chatToDeleteFromMain = _conversations.value.find { it.chatId == chatId }
            val chatToDeleteFromArchive = _archivedConversations.value.find { it.chatId == chatId }

            // Optimistic UI update
            if (chatToDeleteFromMain != null) {
                _conversations.value = _conversations.value.filter { it.chatId != chatId }
            }
            if (chatToDeleteFromArchive != null) {
                _archivedConversations.value = _archivedConversations.value.filter { it.chatId != chatId }
            }

            deleteChatUseCase(chatId, forEveryone).onFailure { e ->
                // Revert on error
                if (chatToDeleteFromMain != null) {
                    _conversations.value = (listOf(chatToDeleteFromMain) + _conversations.value).sortedByDescending { it.lastMessageTime }
                }
                if (chatToDeleteFromArchive != null) {
                    _archivedConversations.value = (listOf(chatToDeleteFromArchive) + _archivedConversations.value).sortedByDescending { it.lastMessageTime }
                }
                _error.value = "Failed to delete chat: ${e.message}"
            }
        }
    }

    private suspend fun loadCurrentUserProfile() {
        try {
            val profile = UserProfileManager.getCurrentUserProfile()
            _currentUserProfile.value = profile
        } catch (_: Exception) { }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            getConversationsUseCase().onSuccess { conversationList ->
                _conversations.value = conversationList
                _isLoading.value = false
                subscribeToInboxUpdates()
            }.onFailure { e ->
                _error.value = "Failed to load conversations: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private var inboxSubscriptionJob: kotlinx.coroutines.Job? = null

    private fun subscribeToInboxUpdates() {
        inboxSubscriptionJob?.cancel()
        val chatIds = _conversations.value.map { it.chatId }
        if (chatIds.isEmpty()) return

        inboxSubscriptionJob = viewModelScope.launch {
            subscribeToInboxUpdatesUseCase(chatIds).collect {
                loadConversations() // Simple reload on any new message
            }
        }
    }

    fun isChatLocked(chatId: String): Boolean = chatLockManager.isChatLocked(chatId)

    fun getFormattedTimestamp(timestamp: String?): String = TimestampFormatter.formatRelative(timestamp)
}
