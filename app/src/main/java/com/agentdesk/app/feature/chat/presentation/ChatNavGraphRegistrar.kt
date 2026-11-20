package com.agentdesk.app.feature.chat.presentation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.chat.presentation.chat.ChatScreen
import com.agentdesk.app.feature.chat.presentation.chat.ChatViewModel
import com.agentdesk.app.feature.chat.presentation.list.ConversationListScreen
import com.agentdesk.app.feature.chat.presentation.list.ConversationListViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatNavGraphRegistrar @Inject constructor() : NavGraphRegistrar {
    override fun register(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable<Routes.ConversationList> {
            val viewModel: ConversationListViewModel = hiltViewModel()
            ConversationListScreen(
                viewModel = viewModel,
                onOpenChat = { id -> navController.navigate(Routes.Chat(id)) },
                onOpenProviders = { navController.navigate(Routes.ProviderList) }
            )
        }

        navGraphBuilder.composable<Routes.Chat> { backStackEntry ->
            val chatRoute = backStackEntry.toRoute<Routes.Chat>()
            val viewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                conversationId = chatRoute.id,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
