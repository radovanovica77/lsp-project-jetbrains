package com.logolsp.analysis

import com.logolsp.parser.LogoLexer
import com.logolsp.parser.LogoParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

object LogoParserFacade {

    fun parse(source: String): LogoParser.ProgramContext {
        val charStream = CharStreams.fromString(source)
        val lexer = LogoLexer(charStream)
        lexer.removeErrorListeners()
        val tokens = CommonTokenStream(lexer)
        val parser = LogoParser(tokens)
        parser.removeErrorListeners()
        return parser.program()
    }

    fun getTokens(source: String): CommonTokenStream {
        val charStream = CharStreams.fromString(source)
        val lexer = LogoLexer(charStream)
        lexer.removeErrorListeners()
        val tokens = CommonTokenStream(lexer)
        tokens.fill()
        return tokens
    }
}