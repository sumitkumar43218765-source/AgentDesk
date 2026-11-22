package com.agentdesk.app.feature.terminal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExtraKeysBar(
    onKeyPress: (String) -> Unit,
    isCtrlActive: Boolean = false,
    isAltActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Official Termux 2-Row Extra Keys Layout
    val row1 = listOf("ESC", "/", "-", "HOME", "▲", "END", "PGUP")
    val row2 = listOf("TAB", "CTRL", "ALT", "◀", "▼", "▶", "PGDN")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141414))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (key in row1) {
                ExtraKeyCell(
                    label = key,
                    isActive = false,
                    onClick = { onKeyPress(key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (key in row2) {
                val isActive = (key == "CTRL" && isCtrlActive) || (key == "ALT" && isAltActive)
                ExtraKeyCell(
                    label = key,
                    isActive = isActive,
                    onClick = { onKeyPress(key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExtraKeyCell(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) Color(0xFF2E7D32) else Color(0xFF242424)
    val textColor = if (isActive) Color.White else Color(0xFFE0E0E0)
    val borderColor = if (isActive) Color(0xFF4CAF50) else Color(0xFF333333)

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
