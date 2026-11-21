package com.agentdesk.app.feature.agent.presentation.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DiffView(
    diffText: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 200
) {
    var expanded by remember { mutableStateOf(false) }
    val lines = remember(diffText) { diffText.lines() }
    val displayLines = if (expanded || lines.size <= maxLines) lines else lines.take(maxLines)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        displayLines.forEach { line ->
            val (bgColor, textColor) = when {
                line.startsWith("+") -> Color(0x334CAF50) to Color(0xFF2E7D32)
                line.startsWith("-") -> Color(0x33F44336) to Color(0xFFC62828)
                line.startsWith("@@") -> Color(0x222196F3) to Color(0xFF1565C0)
                else -> Color.Transparent to MaterialTheme.colorScheme.onSurfaceVariant
            }

            Text(
                text = line,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        if (!expanded && lines.size > maxLines) {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Show more (${lines.size - maxLines} lines omitted)",
                    fontSize = 12.sp
                )
            }
        }
    }
}
