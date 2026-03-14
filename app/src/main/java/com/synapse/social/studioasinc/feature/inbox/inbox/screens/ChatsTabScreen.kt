package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.platform.LocalLayoutDirection
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxShimmer
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.ui.inbox.theme.InboxTheme
import com.synapse.social.studioasinc.feature.inbox.inbox.models.EmptyStateType
import com.synapse.social.studioasinc.domain.model.ChatListLayout
import com.synapse.social.studioasinc.domain.model.ChatSwipeGesture
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import com.synapse.social.studioasinc.R
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTabScreen(
    conversations: List<Conversation>,
    isLoading: Boolean,
    error: String?,
    onConversationClick: (String, String, String?, String?) -> Unit,
    onRetry: () -> Unit,
    isLocked: (String) -> Boolean = { false },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    chatListLayout: ChatListLayout = ChatListLayout.DOUBLE_LINE,
    chatSwipeGesture: ChatSwipeGesture = ChatSwipeGesture.ARCHIVE,
    archivedCount: Int = 0,
    onSwipeAction: (String, ChatSwipeGesture) -> Unit = { _, _ -> },
    onNavigateToArchived: () -> Unit = {}
) {
    var deleteCandidateChatId by remember { mutableStateOf<String?>(null) }
    var longPressedChatId by remember { mutableStateOf<String?>(null) }
    var longPressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var showArchivedRow by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && showArchivedRow) {
                    showArchivedRow = false
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 20f && archivedCount > 0) {
                    showArchivedRow = true
                }
                return Offset.Zero
            }
        }
    }

    if (deleteCandidateChatId != null) {
        DeleteChatDialog(
            onDismissRequest = { deleteCandidateChatId = null },
            onDeleteForMe = {
                onSwipeAction(deleteCandidateChatId!!, ChatSwipeGesture.DELETE)
                deleteCandidateChatId = null
            },
            onDeleteForEveryone = {
                onSwipeAction(deleteCandidateChatId!!, ChatSwipeGesture.DELETE)
                deleteCandidateChatId = null
            }
        )
    }

    when {
        isLoading && conversations.isEmpty() -> {
            InboxShimmer(
                modifier = modifier.fillMaxSize().padding(contentPadding)
            )
        }
        error != null && conversations.isEmpty() -> {
            InboxEmptyState(
                type = EmptyStateType.ERROR,
                message = error,
                onActionClick = onRetry,
                modifier = modifier.fillMaxSize().padding(contentPadding)
            )
        }
        conversations.isEmpty() && archivedCount == 0 -> {
            InboxEmptyState(
                type = EmptyStateType.CHATS,
                modifier = modifier.fillMaxSize().padding(contentPadding)
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
                userScrollEnabled = true,
                contentPadding = PaddingValues(
                    top = Spacing.Small,
                    bottom = Spacing.Small,
                    start = contentPadding.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(androidx.compose.ui.platform.LocalLayoutDirection.current)
                ),
                verticalArrangement = Arrangement.spacedBy(InboxTheme.dimens.GroupedItemGap)
            ) {
                if (archivedCount > 0) {
                    item {
                        AnimatedVisibility(
                            visible = showArchivedRow,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = onNavigateToArchived
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.Medium),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.width(Spacing.Medium))
                                    Text(
                                        text = "Archived Chats ($archivedCount)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(conversations, key = { _, it -> "${it.chatId}_${it.participantId}" }) { index, conversation ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                if (chatSwipeGesture == ChatSwipeGesture.DELETE) deleteCandidateChatId = conversation.chatId else onSwipeAction(conversation.chatId, chatSwipeGesture)
                                true
                            } else false
                        }
                    )

                    val isFirst = index == 0
                    val isLast = index == conversations.size - 1
                    val shape = when {
                        conversations.size == 1 -> InboxTheme.shapes.GroupedListSingleShape
                        isFirst -> InboxTheme.shapes.GroupedListTopShape
                        isLast -> InboxTheme.shapes.GroupedListBottomShape
                        else -> InboxTheme.shapes.GroupedListMiddleShape
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = Spacing.Medium)
                                        .clip(shape)
                                        .background(color, shape)
                                        .padding(horizontal = Spacing.Large),
                                    contentAlignment = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        else -> Alignment.Center
                                    }
                                ) {
                                    if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                        Icon(
                                            imageVector = when (dismissState.targetValue) {
                                                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Archive
                                                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                                                else -> Icons.Filled.Archive
                                            },
                                            contentDescription = null,
                                            tint = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            content = {
                                ConversationItem(
                                    conversation = conversation,
                                    isLocked = isLocked(conversation.chatId),
                                    onClick = {
                                        if (conversation.isGroup) {
                                            onConversationClick(conversation.chatId, "", conversation.participantName, conversation.participantAvatar)
                                        } else {
                                            onConversationClick(conversation.chatId, conversation.participantId, conversation.participantName, conversation.participantAvatar)
                                        }
                                    },
                                    onLongPress = { offset ->
                                        longPressedChatId = conversation.chatId
                                        longPressOffset = DpOffset(with(density) { offset.x.toDp() }, with(density) { offset.y.toDp() })
                                    },
                                    layout = chatListLayout,
                                    shape = shape
                                )
                            }
                        )

                        if (longPressedChatId == conversation.chatId) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { longPressedChatId = null },
                                offset = longPressOffset
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Archive") },
                                    leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                                    onClick = {
                                        longPressedChatId = null
                                        onSwipeAction(conversation.chatId, ChatSwipeGesture.ARCHIVE)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        longPressedChatId = null
                                        deleteCandidateChatId = conversation.chatId
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    isLocked: Boolean,
    onClick: () -> Unit,
    onLongPress: (Offset) -> Unit = {},
    layout: ChatListLayout = ChatListLayout.DOUBLE_LINE,
    shape: Shape = InboxTheme.shapes.ChatItemCard
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .pointerInput(conversation.chatId) {
                detectTapGestures(
                    onLongPress = { onLongPress(it) },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box {
            AsyncImage(
                model = conversation.participantAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(InboxTheme.dimens.AvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentScale = ContentScale.Crop
            )
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(InboxTheme.dimens.OnlineIndicatorSize)
                        .clip(CircleShape)
                        .background(StatusOnline, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(Spacing.Medium).padding(end = Spacing.ExtraSmall),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = conversation.participantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatTimestamp(conversation.lastMessageTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (layout == ChatListLayout.DOUBLE_LINE) {
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(InboxTheme.dimens.UnreadBadgeSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small)
                    )

                    if (conversation.unreadCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(InboxTheme.dimens.UnreadBadgeSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats an ISO 8601 timestamp into a human-friendly relative string.
 */
private fun formatTimestamp(isoTimestamp: String?): String {
    if (isoTimestamp == null) return ""
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val minutesAgo = ChronoUnit.MINUTES.between(instant, now)
        val hoursAgo = ChronoUnit.HOURS.between(instant, now)
        val daysAgo = ChronoUnit.DAYS.between(instant, now)

        when {
            minutesAgo < 1 -> "Just now"
            minutesAgo < 60 -> "${minutesAgo}m"
            hoursAgo < 24 -> "${hoursAgo}h"
            daysAgo < 7 -> {
                val formatter = DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        ""
    }
}


@Composable
fun DeleteChatDialog(
    onDismissRequest: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Delete chat") },
        text = {
            Column {
                Text("Are you sure you want to delete this chat?")
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDeleteForEveryone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete for everyone")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDeleteForMe,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete for me")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}
