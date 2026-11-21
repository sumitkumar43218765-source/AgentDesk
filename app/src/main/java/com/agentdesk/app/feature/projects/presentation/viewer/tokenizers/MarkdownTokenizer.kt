package com.agentdesk.app.feature.projects.presentation.viewer.tokenizers

class MarkdownTokenizer : CodeTokenizer {

    override fun tokenize(code: String): List<AnnotatedToken> {
        val tokens = mutableListOf<AnnotatedToken>()
        val lines = code.lines()

        for (line in lines) {
            if (line.startsWith("#")) {
                tokens.add(AnnotatedToken(line + "\n", TokenType.KEYWORD))
            } else if (line.startsWith("```")) {
                tokens.add(AnnotatedToken(line + "\n", TokenType.STRING))
            } else {
                tokens.add(AnnotatedToken(line + "\n", TokenType.DEFAULT))
            }
        }

        return tokens
    }
}
