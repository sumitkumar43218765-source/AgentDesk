package com.agentdesk.app.feature.chat.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agentdesk.app.R
import com.agentdesk.app.feature.ai.domain.model.TokenUsage

@Composable
fun UsageFootnote(
    usage: TokenUsage,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.tokens_usage, usage.inputTokens, usage.outputTokens),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(top = 4.dp, start = 4.dp)
    )
}
