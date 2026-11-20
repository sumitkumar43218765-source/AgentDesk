package com.agentdesk.app.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.agentdesk.app.R
import com.agentdesk.app.core.ui.components.AppScaffold
import com.agentdesk.app.core.ui.components.AppTopBar
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeNavGraphRegistrar @Inject constructor() : NavGraphRegistrar {
    override fun register(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable<Routes.Home> {
            HomeScreen(
                onNavigateToChat = { navController.navigate(Routes.ConversationList) },
                onNavigateToProviders = { navController.navigate(Routes.ProviderList) },
                onNavigateToTerminal = { navController.navigate(Routes.Terminal) }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit = {},
    onNavigateToProviders: () -> Unit = {},
    onNavigateToTerminal: () -> Unit = {}
) {
    AppScaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.home_title))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusChip(
                text = stringResource(R.string.home_status_ready),
                level = StatusLevel.SUCCESS
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Chat")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToTerminal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Terminal")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToProviders,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configure AI Providers")
            }
        }
    }
}
