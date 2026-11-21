package com.agentdesk.app.feature.projects.presentation.viewer.tokenizers

class JavaTokenizer : CodeTokenizer {

    private val keywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null"
    )

    override fun tokenize(code: String): List<AnnotatedToken> {
        val tokens = mutableListOf<AnnotatedToken>()
        var i = 0
        val n = code.length

        while (i < n) {
            val c = code[i]

            // Line comment //
            if (c == '/' && i + 1 < n && code[i + 1] == '/') {
                val start = i
                while (i < n && code[i] != '\n') i++
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.COMMENT))
                continue
            }

            // String "..."
            if (c == '"') {
                val start = i
                i++
                while (i < n && code[i] != '"') {
                    if (code[i] == '\\' && i + 1 < n) i++
                    i++
                }
                if (i < n) i++ // include closing quote
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.STRING))
                continue
            }

            // Word / identifier
            if (c.isLetter() || c == '_') {
                val start = i
                while (i < n && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                val word = code.substring(start, i)
                val type = if (keywords.contains(word)) TokenType.KEYWORD else TokenType.DEFAULT
                tokens.add(AnnotatedToken(word, type))
                continue
            }

            // Number
            if (c.isDigit()) {
                val start = i
                while (i < n && (code[i].isLetterOrDigit() || code[i] == '.')) i++
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.NUMBER))
                continue
            }

            // Other character
            tokens.add(AnnotatedToken(c.toString(), TokenType.DEFAULT))
            i++
        }

        return tokens
    }
}
