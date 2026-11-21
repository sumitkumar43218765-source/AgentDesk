package com.agentdesk.app.feature.projects.presentation.create.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.agentdesk.app.core.ui.components.StatusChip
import com.agentdesk.app.core.ui.components.StatusLevel
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.projects.domain.model.TemplateId

@Composable
fun TemplatePicker(
    selectedTemplate: TemplateId,
    onSelectTemplate: (TemplateId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Template",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.small))

        TemplateOption(
            title = "Hello Java (Recommended)",
            description = "Standard Android Views (XML layouts + Java source)",
            selected = selectedTemplate == TemplateId.HELLO_JAVA,
            enabled = true,
            onClick = { onSelectTemplate(TemplateId.HELLO_JAVA) }
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        TemplateOption(
            title = "Empty Project",
            description = "Minimal project structure without predefined layouts",
            selected = selectedTemplate == TemplateId.EMPTY,
            enabled = true,
            onClick = { onSelectTemplate(TemplateId.EMPTY) }
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        TemplateOption(
            title = "Hello JNI (C/C++)",
            description = "Native ARM64 libraries with CMake",
            selected = false,
            enabled = false,
            badge = "Coming later",
            onClick = {}
        )
    }
}

@Composable
private fun TemplateOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = shape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(Spacing.small))
                        StatusChip(text = badge, level = StatusLevel.INFO)
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
