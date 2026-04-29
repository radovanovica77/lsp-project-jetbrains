package com.logolsp.server

import com.logolsp.analysis.AnalysisResult
import com.logolsp.analysis.DocumentManager
import com.logolsp.analysis.LogoParserFacade
import com.logolsp.analysis.SemanticAnalyzer
import com.logolsp.features.DefinitionProvider
import com.logolsp.features.DiagnosticsProvider
import com.logolsp.features.SemanticTokensProvider
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture

class LogoTextDocumentService(
    private val documentManager: DocumentManager
) : TextDocumentService {

    private lateinit var client: LanguageClient
    private val analyzer = SemanticAnalyzer()
    private val semanticTokensProvider = SemanticTokensProvider()
    private val definitionProvider = DefinitionProvider()
    private val diagnosticsProvider = DiagnosticsProvider()

    // Keš poslednjeg rezultata analize po URI-ju
    // Izbegavamo duplo parsiranje za isti sadržaj
    private val analysisCache = mutableMapOf<String, AnalysisResult>()

    fun setClient(client: LanguageClient) {
        this.client = client
    }

    private fun analyzeAndPublish(uri: String, content: String) {
        val tree = LogoParserFacade.parse(content)
        val result = analyzer.analyze(tree, uri)
        analysisCache[uri] = result  // sačuvaj u keš
        val diagnostics = diagnosticsProvider.provide(result)
        client.publishDiagnostics(PublishDiagnosticsParams(uri, diagnostics))
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        val uri = params.textDocument.uri
        val content = params.textDocument.text
        documentManager.update(uri, content)
        analyzeAndPublish(uri, content)
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val uri = params.textDocument.uri
        val content = params.contentChanges.last().text
        documentManager.update(uri, content)
        analyzeAndPublish(uri, content)
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        val uri = params.textDocument.uri
        documentManager.remove(uri)
        analysisCache.remove(uri)  // očisti keš
    }

    override fun didSave(params: DidSaveTextDocumentParams) {}

    override fun semanticTokensFull(
        params: SemanticTokensParams
    ): CompletableFuture<SemanticTokens> {
        val content = documentManager.get(params.textDocument.uri) ?: ""
        return CompletableFuture.completedFuture(
            semanticTokensProvider.provide(content)
        )
    }

    override fun definition(
        params: DefinitionParams
    ): CompletableFuture<Either<List<Location>, List<LocationLink>>> {
        val uri = params.textDocument.uri
        // Koristi keširani rezultat umesto ponovnog parsiranja
        val result = analysisCache[uri] ?: run {
            val content = documentManager.get(uri) ?: ""
            val tree = LogoParserFacade.parse(content)
            analyzer.analyze(tree, uri)
        }
        val location = definitionProvider.provide(
            result, uri,
            params.position.line,
            params.position.character
        )
        val locations = if (location != null) listOf(location) else emptyList()
        return CompletableFuture.completedFuture(Either.forLeft(locations))
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover?> {
        val uri = params.textDocument.uri
        val content = documentManager.get(uri)
            ?: return CompletableFuture.completedFuture(null)

        val line = params.position.line
        val character = params.position.character

        // Koristi keširani rezultat
        val result = analysisCache[uri] ?: run {
            val tree = LogoParserFacade.parse(content)
            analyzer.analyze(tree, uri)
        }

        val ref = result.references.firstOrNull { ref ->
            ref.line == line &&
                    character >= ref.column &&
                    character <= ref.column + ref.name.length
        }

        if (ref != null) {
            val symbol = result.symbols.firstOrNull {
                it.name == ref.name && it.kind == ref.kind
            }
            if (symbol != null) {
                val kindStr = if (symbol.kind == com.logolsp.analysis.SymbolKind.PROCEDURE)
                    "procedure" else "variable"
                val msg = "**${symbol.name}** — user-defined $kindStr, declared at line ${symbol.line + 1}"
                return CompletableFuture.completedFuture(
                    Hover(MarkupContent("markdown", msg))
                )
            }
        }

        val builtinDocs = mapOf(
            "FORWARD"     to "**FORWARD** `FORWARD distance`\n\nMoves the turtle forward.",
            "FD"          to "**FD** `FD distance`\n\nShorthand for FORWARD.",
            "BACK"        to "**BACK** `BACK distance`\n\nMoves the turtle backward.",
            "BK"          to "**BK** `BK distance`\n\nShorthand for BACK.",
            "RIGHT"       to "**RIGHT** `RIGHT degrees`\n\nTurns the turtle right.",
            "RT"          to "**RT** `RT degrees`\n\nShorthand for RIGHT.",
            "LEFT"        to "**LEFT** `LEFT degrees`\n\nTurns the turtle left.",
            "LT"          to "**LT** `LT degrees`\n\nShorthand for LEFT.",
            "PENUP"       to "**PENUP** `PENUP`\n\nLifts the pen — turtle moves without drawing.",
            "PU"          to "**PU** `PU`\n\nShorthand for PENUP.",
            "PENDOWN"     to "**PENDOWN** `PENDOWN`\n\nPuts the pen down — turtle draws while moving.",
            "PD"          to "**PD** `PD`\n\nShorthand for PENDOWN.",
            "HOME"        to "**HOME** `HOME`\n\nMoves turtle to center and resets direction.",
            "CLEARSCREEN" to "**CLEARSCREEN** `CLEARSCREEN`\n\nClears the screen and resets the turtle.",
            "CS"          to "**CS** `CS`\n\nShorthand for CLEARSCREEN.",
            "REPEAT"      to "**REPEAT** `REPEAT n [ commands ]`\n\nRepeats commands n times.",
            "IF"          to "**IF** `IF condition [ commands ]`\n\nExecutes block if condition is true.",
            "ELSE"        to "**ELSE** `ELSE [ commands ]`\n\nExecuted when IF condition is false.",
            "MAKE"        to "**MAKE** `MAKE \"name value`\n\nAssigns a value to a variable.",
            "PRINT"       to "**PRINT** `PRINT value`\n\nPrints a value to the output.",
            "SHOW"        to "**SHOW** `SHOW value`\n\nDisplays a value.",
            "TO"          to "**TO** `TO name :param1 :param2`\n\nBegins a procedure definition.",
            "END"         to "**END**\n\nEnds a procedure definition."
        )

        val tokenStream = LogoParserFacade.getTokens(content)
        val token = tokenStream.tokens.firstOrNull { t ->
            if (t.type == org.antlr.v4.runtime.Token.EOF) return@firstOrNull false
            val tokenLine = t.line - 1
            val tokenStart = t.charPositionInLine
            val tokenEnd = tokenStart + t.text.length
            tokenLine == line && character >= tokenStart && character < tokenEnd
        }

        if (token != null) {
            val doc = builtinDocs[token.text.uppercase()]
            if (doc != null) {
                return CompletableFuture.completedFuture(
                    Hover(MarkupContent("markdown", doc))
                )
            }
            if (token.type == com.logolsp.parser.LogoLexer.NUMBER) {
                return CompletableFuture.completedFuture(
                    Hover(MarkupContent("markdown", "**Number literal:** `${token.text}`"))
                )
            }
        }

        return CompletableFuture.completedFuture(null)
    }
}