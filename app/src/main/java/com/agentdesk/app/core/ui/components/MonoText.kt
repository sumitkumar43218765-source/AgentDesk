package com.agentdesk.app.core.ui.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.agentdesk.app.core.ui.theme.MonospaceTextStyle

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MonospaceTextStyle,
    selectable: Boolean = false
) {
    if (selectable) {
        SelectionContainer {
            Text(
                text = text,
                modifier = modifier,
                color = color,
                style = style
            )
        }
    } else {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            style = style
        )
    }
}
