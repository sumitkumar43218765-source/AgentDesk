package com.agentdesk.app.feature.projects.presentation.viewer.tokenizers

class ShellTokenizer : CodeTokenizer {

    private val keywords = setOf("if", "then", "else", "elif", "fi", "for", "while", "do", "done", "in", "case", "esac", "echo", "exit", "return")

    override fun tokenize(code: String): List<AnnotatedToken> {
        val tokens = mutableListOf<AnnotatedToken>()
        var i = 0
        val n = code.length

        while (i < n) {
            val c = code[i]

            // Comment #
            if (c == '#') {
                val start = i
                while (i < n && code[i] != '\n') i++
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.COMMENT))
                continue
            }

            // String "..." or '...'
            if (c == '"' || c == '\'') {
                val quote = c
                val start = i
                i++
                while (i < n && code[i] != quote) {
                    if (code[i] == '\\' && i + 1 < n) i++
                    i++
                }
                if (i < n) i++
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.STRING))
                continue
            }

            // Variable $VAR
            if (c == '$') {
                val start = i
                i++
                while (i < n && (code[i].isLetterOrDigit() || code[i] == '_' || code[i] == '{' || code[i] == '}')) i++
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.ATTRIBUTE))
                continue
            }

            // Word
            if (c.isLetter() || c == '_') {
                val start = i
                while (i < n && (code[i].isLetterOrDigit() || code[i] == '_' || code[i] == '-')) i++
                val word = code.substring(start, i)
                val type = if (keywords.contains(word)) TokenType.KEYWORD else TokenType.DEFAULT
                tokens.add(AnnotatedToken(word, type))
                continue
            }

            tokens.add(AnnotatedToken(c.toString(), TokenType.DEFAULT))
            i++
        }

        return tokens
    }
}
