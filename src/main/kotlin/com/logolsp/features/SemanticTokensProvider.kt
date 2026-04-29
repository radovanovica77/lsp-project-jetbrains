package com.logolsp.features

import com.logolsp.analysis.LogoParserFacade
import com.logolsp.parser.LogoLexer
import org.antlr.v4.runtime.Token
import org.eclipse.lsp4j.SemanticTokens

class SemanticTokensProvider {

    private val keywordTypes = setOf(
        LogoLexer.TO, LogoLexer.END, LogoLexer.REPEAT, LogoLexer.IF,
        LogoLexer.ELSE, LogoLexer.MAKE, LogoLexer.FORWARD, LogoLexer.BACK,
        LogoLexer.RIGHT, LogoLexer.LEFT, LogoLexer.PENUP, LogoLexer.PENDOWN,
        LogoLexer.HOME, LogoLexer.CLEARSCREEN, LogoLexer.PRINT, LogoLexer.SHOW
    )

    fun provide(source: String): SemanticTokens {
        val tokenStream = LogoParserFacade.getTokens(source)
        val tokens = tokenStream.tokens
        val data = mutableListOf<Int>()

        var prevLine = 0
        var prevChar = 0
        var i = 0

        while (i < tokens.size) {
            val token = tokens[i]
            if (token.type == Token.EOF) break

            val line = token.line - 1
            val char = token.charPositionInLine

            // Varijabla: COLON + IDENT zajedno (lookahead)
            if (token.type == LogoLexer.COLON && i + 1 < tokens.size) {
                val next = tokens[i + 1]
                if (next.type == LogoLexer.IDENT) {
                    val length = token.text.length + next.text.length
                    val deltaLine = line - prevLine
                    val deltaChar = if (deltaLine == 0) char - prevChar else char
                    data.addAll(listOf(deltaLine, deltaChar, length, 2, 0)) // 2 = variable
                    prevLine = line
                    prevChar = char
                    i += 2
                    continue
                }
            }

            // Keyword
            if (token.type in keywordTypes) {
                val deltaLine = line - prevLine
                val deltaChar = if (deltaLine == 0) char - prevChar else char
                data.addAll(listOf(deltaLine, deltaChar, token.text.length, 0, 0)) // 0 = keyword
                prevLine = line
                prevChar = char
                i++
                continue
            }

            // Identifier (procedure name)
            if (token.type == LogoLexer.IDENT) {
                val deltaLine = line - prevLine
                val deltaChar = if (deltaLine == 0) char - prevChar else char
                data.addAll(listOf(deltaLine, deltaChar, token.text.length, 1, 0)) // 1 = function
                prevLine = line
                prevChar = char
                i++
                continue
            }

            // Number
            if (token.type == LogoLexer.NUMBER) {
                val deltaLine = line - prevLine
                val deltaChar = if (deltaLine == 0) char - prevChar else char
                data.addAll(listOf(deltaLine, deltaChar, token.text.length, 3, 0)) // 3 = number
                prevLine = line
                prevChar = char
                i++
                continue
            }

            i++
        }

        return SemanticTokens(data)
    }
}