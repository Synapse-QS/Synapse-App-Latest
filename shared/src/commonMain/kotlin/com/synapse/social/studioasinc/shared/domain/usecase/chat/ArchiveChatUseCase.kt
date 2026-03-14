package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class ArchiveChatUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String): Result<Unit> {
        return repository.archiveChat(chatId)
    }
}
