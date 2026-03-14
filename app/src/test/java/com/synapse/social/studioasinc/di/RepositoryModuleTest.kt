package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.core.di.RepositoryModule
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock

class RepositoryModuleTest {

    private val mockChatRepository = mock(ChatRepository::class.java)

    @Test
    fun `provideArchiveChatUseCase returns non-null instance`() {
        val useCase = RepositoryModule.provideArchiveChatUseCase(mockChatRepository)
        assertNotNull(useCase)
    }

    @Test
    fun `provideUnarchiveChatUseCase returns non-null instance`() {
        val useCase = RepositoryModule.provideUnarchiveChatUseCase(mockChatRepository)
        assertNotNull(useCase)
    }

    @Test
    fun `provideDeleteChatUseCase returns non-null instance`() {
        val useCase = RepositoryModule.provideDeleteChatUseCase(mockChatRepository)
        assertNotNull(useCase)
    }

    @Test
    fun `provideGetArchivedConversationsUseCase returns non-null instance`() {
        val useCase = RepositoryModule.provideGetArchivedConversationsUseCase(mockChatRepository)
        assertNotNull(useCase)
    }
}
