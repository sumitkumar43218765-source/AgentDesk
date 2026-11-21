package com.agentdesk.app.feature.projects.presentation.viewer.tokenizers

class JsonTokenizer : CodeTokenizer {

    override fun tokenize(code: String): List<AnnotatedToken> {
        val tokens = mutableListOf<AnnotatedToken>()
        var i = 0
        val n = code.length

        while (i < n) {
            val c = code[i]

            // String "..."
            if (c == '"') {
                val start = i
                i++
                while (i < n && code[i] != '"') {
                    if (code[i] == '\\' && i + 1 < n) i++
                    i++
                }
                if (i < n) i++
                val str = code.substring(start, i)

                // Check if followed by colon (key)
                var j = i
                while (j < n && code[j].isWhitespace()) j++
                val type = if (j < n && code[j] == ':') TokenType.ATTRIBUTE else TokenType.STRING
                tokens.add(AnnotatedToken(str, type))
                continue
            }

            // Number
            if (c.isDigit() || (c == '-' && i + 1 < n && code[i + 1].isDigit())) {
                val start = i
                i++
                while (i < n && (code[i].isDigit() || code[i] == '.' || code[i] == 'e' || code[i] == 'E' || code[i] == '+' || code[i] == '-')) i++
                tokens.add(AnnotatedToken(code.substring(start, i), TokenType.NUMBER))
                continue
            }

            // Keywords: true, false, null
            if (c.isLetter()) {
                val start = i
                while (i < n && code[i].isLetter()) i++
                val word = code.substring(start, i)
                val type = if (word == "true" || word == "false" || word == "null") TokenType.KEYWORD else TokenType.DEFAULT
                tokens.add(AnnotatedToken(word, type))
                continue
            }

            tokens.add(AnnotatedToken(c.toString(), TokenType.DEFAULT))
            i++
        }

        return tokens
    }
}
