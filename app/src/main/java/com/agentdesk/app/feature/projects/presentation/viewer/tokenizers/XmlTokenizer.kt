package com.agentdesk.app.feature.projects.presentation.viewer.tokenizers

class XmlTokenizer : CodeTokenizer {

    override fun tokenize(code: String): List<AnnotatedToken> {
        val tokens = mutableListOf<AnnotatedToken>()
        var i = 0
        val n = code.length

        while (i < n) {
            val c = code[i]

            // Comment <!-- ... -->
            if (code.startsWith("<!--", i)) {
                val end = code.indexOf("-->", i)
                val tokenEnd = if (end != -1) end + 3 else n
                tokens.add(AnnotatedToken(code.substring(i, tokenEnd), TokenType.COMMENT))
                i = tokenEnd
                continue
            }

            // String attribute value "..."
            if (c == '"') {
                val start = i
                i++
                while (i < n && code[i] != '"') i++
                if (i < n) i++
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.STRING))
                continue
            }

            // Tag brackets <, </, />, >
            if (c == '<') {
                val start = i
                i++
                if (i < n && code[i] == '/') i++
                while (i < n && (code[i].isLetterOrDigit() || code[i] == ':' || code[i] == '_')) i++
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.TAG))
                continue
            }

            if (c == '>' || (c == '/' && i + 1 < n && code[i + 1] == '>')) {
                val len = if (c == '/') 2 else 1
                tokens.add(AnnotatedToken(code.substring(i, i + len), TokenType.TAG))
                i += len
                continue
            }

            // Attribute name
            if (c.isLetter() || c == ':') {
                val start = i
                while (i < n && (code[i].isLetterOrDigit() || code[i] == ':' || code[i] == '-' || code[i] == '_')) i++
                val word = code.substring(start, i)
                tokens.add(AnnotatedToken(word, TokenType.ATTRIBUTE))
                continue
            }

            tokens.add(AnnotatedToken(c.toString(), TokenType.DEFAULT))
            i++
        }

        return tokens
    }
}
