package com.logolsp.analysis

import com.logolsp.parser.LogoParser

data class Symbol(
    val name: String,
    val kind: SymbolKind,
    val line: Int,
    val column: Int,
    val uri: String
)

enum class SymbolKind { PROCEDURE, VARIABLE }

data class Diagnostic(
    val message: String,
    val line: Int,
    val column: Int,
    val length: Int
)

data class Reference(
    val name: String,
    val kind: SymbolKind,
    val line: Int,
    val column: Int,
    val uri: String
)

data class AnalysisResult(
    val symbols: List<Symbol>,
    val references: List<Reference>,
    val diagnostics: List<Diagnostic>
)

class SemanticAnalyzer {

    private val builtins = setOf(
        "FORWARD", "FD", "BACK", "BK", "RIGHT", "RT", "LEFT", "LT",
        "PENUP", "PU", "PENDOWN", "PD", "HOME", "CLEARSCREEN", "CS",
        "PRINT", "SHOW", "REPEAT", "IF", "ELSE", "MAKE"
    )

    fun analyze(tree: LogoParser.ProgramContext, uri: String): AnalysisResult {
        val symbols = mutableListOf<Symbol>()
        val diagnostics = mutableListOf<Diagnostic>()
        val references = mutableListOf<Reference>()

        // Prvi prolaz — prikupi sve definicije
        for (line in tree.line()) {
            val stmt = line.statement() ?: continue
            val procDef = stmt.procedureDefinition() ?: continue
            val nameToken = procDef.IDENT()
            symbols.add(Symbol(
                name = nameToken.text.uppercase(),
                kind = SymbolKind.PROCEDURE,
                line = nameToken.symbol.line - 1,
                column = nameToken.symbol.charPositionInLine,
                uri = uri
            ))
            for (param in procDef.parameter()) {
                val paramToken = param.IDENT()
                symbols.add(Symbol(
                    name = paramToken.text.uppercase(),
                    kind = SymbolKind.VARIABLE,
                    line = paramToken.symbol.line - 1,
                    column = paramToken.symbol.charPositionInLine,
                    uri = uri
                ))
            }
        }

        // Drugi prolaz — proveri reference
        val stmts = tree.line().mapNotNull { it.statement() }
        walkStatements(stmts, symbols, references, diagnostics, uri)

        return AnalysisResult(symbols, references, diagnostics)
    }

    private fun walkStatements(
        stmts: List<LogoParser.StatementContext>,
        symbols: List<Symbol>,
        references: MutableList<Reference>,
        diagnostics: MutableList<Diagnostic>,
        uri: String
    ) {
        val symbolNames = symbols.map { it.name }.toSet()

        for (stmt in stmts) {
            val procDef = stmt.procedureDefinition()
            if (procDef != null) {
                val innerStmts = procDef.statement()
                walkStatements(innerStmts, symbols, references, diagnostics, uri)
                continue
            }

            val cmd = stmt.command() ?: continue

            val procCall = cmd.procedureCall()
            if (procCall != null) {
                val nameToken = procCall.IDENT()
                val name = nameToken.text.uppercase()
                if (name !in builtins && name !in symbolNames) {
                    diagnostics.add(Diagnostic(
                        message = "Undefined procedure: '$name'",
                        line = nameToken.symbol.line - 1,
                        column = nameToken.symbol.charPositionInLine,
                        length = name.length
                    ))
                } else {
                    references.add(Reference(
                        name = name,
                        kind = SymbolKind.PROCEDURE,
                        line = nameToken.symbol.line - 1,
                        column = nameToken.symbol.charPositionInLine,
                        uri = uri
                    ))
                }
            }

            collectVariableRefs(cmd, symbols, references, diagnostics, uri)
        }
    }

    private fun collectVariableRefs(
        ctx: org.antlr.v4.runtime.ParserRuleContext,
        symbols: List<Symbol>,
        references: MutableList<Reference>,
        diagnostics: MutableList<Diagnostic>,
        uri: String
    ) {
        for (child in ctx.children ?: emptyList()) {
            if (child is LogoParser.VariableExprContext) {
                val nameToken = child.IDENT()
                val name = nameToken.text.uppercase()
                val symbol = symbols.firstOrNull {
                    it.name == name && it.kind == SymbolKind.VARIABLE
                }
                if (symbol == null) {
                    diagnostics.add(Diagnostic(
                        message = "Undefined variable: ':$name'",
                        line = nameToken.symbol.line - 1,
                        column = nameToken.symbol.charPositionInLine - 1,
                        length = name.length + 1
                    ))
                } else {
                    references.add(Reference(
                        name = name,
                        kind = SymbolKind.VARIABLE,
                        line = nameToken.symbol.line - 1,
                        column = nameToken.symbol.charPositionInLine - 1,
                        uri = uri
                    ))
                }
            } else if (child is org.antlr.v4.runtime.ParserRuleContext) {
                collectVariableRefs(child, symbols, references, diagnostics, uri)
            }
        }
    }
}