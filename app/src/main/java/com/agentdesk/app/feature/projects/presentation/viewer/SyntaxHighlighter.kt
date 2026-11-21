package com.agentdesk.app.feature.projects.presentation.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.agentdesk.app.feature.projects.presentation.viewer.tokenizers.CodeTokenizer
import com.agentdesk.app.feature.projects.presentation.viewer.tokenizers.JavaTokenizer
import com.agentdesk.app.feature.projects.presentation.viewer.tokenizers.JsonTokenizer
import com.agentdesk.app.feature.projects.presentation.viewer.tokenizers.MarkdownTokenizer
import com.agentdesk.app.feature.projects.presentation.viewer.tokenizers.ShellTokenizer
import com.agentdesk.app.feature.projects.presentation.viewer.tokenizers.TokenType
import com.agentdesk.app.feature.projects.presentation.viewer.tokenizers.XmlTokenizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyntaxHighlighter @Inject constructor() {

    private val javaTokenizer = JavaTokenizer()
    private val xmlTokenizer = XmlTokenizer()
    private val jsonTokenizer = JsonTokenizer()
    private val markdownTokenizer = MarkdownTokenizer()
    private val shellTokenizer = ShellTokenizer()

    fun highlight(code: String, filePath: String): AnnotatedString {
        val tokenizer: CodeTokenizer = when {
            filePath.endsWith(".java") || filePath.endsWith(".kt") || filePath.endsWith(".gradle") -> javaTokenizer
            filePath.endsWith(".xml") -> xmlTokenizer
            filePath.endsWith(".json") -> jsonTokenizer
            filePath.endsWith(".md") -> markdownTokenizer
            filePath.endsWith(".sh") || filePath.endsWith(".env") || filePath.endsWith(".properties") -> shellTokenizer
            else -> javaTokenizer
        }

        val tokens = tokenizer.tokenize(code)
        return buildAnnotatedString {
            for (token in tokens) {
                val color = when (token.type) {
                    TokenType.KEYWORD -> Color(0xFFCC7832)
                    TokenType.STRING -> Color(0xFF6A8759)
                    TokenType.COMMENT -> Color(0xFF808080)
                    TokenType.NUMBER -> Color(0xFF6897BB)
                    TokenType.TYPE -> Color(0xFF9876AA)
                    TokenType.TAG -> Color(0xFFE8BF6A)
                    TokenType.ATTRIBUTE -> Color(0xFF9876AA)
                    TokenType.DEFAULT -> Color(0xFFA9B7C6)
                }

                val isBold = token.type == TokenType.KEYWORD || token.type == TokenType.TAG
                val start = length
                append(token.text)
                addStyle(
                    SpanStyle(
                        color = color,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                    ),
                    start,
                    length
                )
            }
        }
    }
}
