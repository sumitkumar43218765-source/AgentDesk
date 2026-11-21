package com.agentdesk.app.feature.projects.presentation.viewer.tokenizers

enum class TokenType {
    KEYWORD,
    STRING,
    COMMENT,
    NUMBER,
    TYPE,
    TAG,
    ATTRIBUTE,
    DEFAULT
}

data class AnnotatedToken(
    val text: String,
    val type: TokenType
)

interface CodeTokenizer {
    fun tokenize(code: String): List<AnnotatedToken>
}
