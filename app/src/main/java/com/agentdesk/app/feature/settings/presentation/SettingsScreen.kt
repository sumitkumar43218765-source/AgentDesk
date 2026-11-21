package com.agentdesk.app.feature.settings.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.agentdesk.app.feature.settings.domain.model.SettingsSection

data class SettingsSectionItem(
    val section: SettingsSection,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToSection: (SettingsSection) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = listOf(
        SettingsSectionItem(
            section = SettingsSection.PROVIDERS,
            title = "Providers",
            description = "AI providers, API keys, default models",
            icon = Icons.Default.Key
        ),
        SettingsSectionItem(
            section = SettingsSection.AGENT,
            title = "Agent",
            description = "Run mode, step limit, timeout, permissions",
            icon = Icons.Default.SmartToy
        ),
        SettingsSectionItem(
            section = SettingsSection.ENVIRONMENT,
            title = "Environment",
            description = "Toolchain manager, apt mirror, repair",
            icon = Icons.Default.Build
        ),
        SettingsSectionItem(
            section = SettingsSection.PROJECTS,
            title = "Projects",
            description = "Git author identity, auto-checkpoint",
            icon = Icons.Default.Folder
        ),
        SettingsSectionItem(
            section = SettingsSection.TERMINAL,
            title = "Terminal",
            description = "Font size, keep screen on, extra keys",
            icon = Icons.Default.Terminal
        ),
        SettingsSectionItem(
            section = SettingsSection.APPEARANCE,
            title = "Appearance",
            description = "Theme mode, dynamic Material You color",
            icon = Icons.Default.Palette
        ),
        SettingsSectionItem(
            section = SettingsSection.NOTIFICATIONS,
            title = "Notifications",
            description = "Notification channels and alerts",
            icon = Icons.Default.Notifications
        ),
        SettingsSectionItem(
            section = SettingsSection.PERMISSIONS,
            title = "Permissions",
            description = "Android runtime permissions and status",
            icon = Icons.Default.Security
        ),
        SettingsSectionItem(
            section = SettingsSection.DATA_PRIVACY,
            title = "Data & Privacy",
            description = "Export diagnostics, wipe workspace",
            icon = Icons.Default.PrivacyTip
        ),
        SettingsSectionItem(
            section = SettingsSection.ABOUT,
            title = "About",
            description = "Version, licenses, upstream Termux tag",
            icon = Icons.Default.Info
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(sections, key = { it.section.name }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigateToSection(item.section) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open ${item.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
