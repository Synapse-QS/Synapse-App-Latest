package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class DeleteChatUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, forEveryone: Boolean): Result<Unit> {
        return repository.deleteChat(chatId, forEveryone)
    }
}
