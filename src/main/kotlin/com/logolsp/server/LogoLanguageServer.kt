package com.logolsp.server

import com.logolsp.analysis.DocumentManager
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.jsonrpc.messages.Either

class LogoLanguageServer : LanguageServer, LanguageClientAware {

    private val documentManager = DocumentManager()
    private lateinit var client: LanguageClient
    private val textDocumentService = LogoTextDocumentService(documentManager)
    private val workspaceService = LogoWorkspaceService()

    override fun connect(client: LanguageClient) {
        this.client = client
        textDocumentService.setClient(client)
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        val capabilities = ServerCapabilities()

        // Syntax highlighting
        capabilities.semanticTokensProvider = SemanticTokensWithRegistrationOptions().apply {
            legend = SemanticTokensLegend(
                listOf("keyword", "function", "variable", "number", "string", "comment", "operator"),
                listOf("declaration", "definition")
            )
            setFull(true)
        }

        // Go-to-declaration
        capabilities.definitionProvider = Either.forLeft(true)

        // Diagnostics se šalju automatski, ne trebaju capability

        // Hover
        capabilities.hoverProvider = Either.forLeft(true)

        capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full)

        return CompletableFuture.completedFuture(InitializeResult(capabilities))
    }

    override fun initialized(params: InitializedParams) {}

    override fun shutdown(): CompletableFuture<Any> {
        return CompletableFuture.completedFuture(null)
    }

    override fun exit() {}

    override fun getTextDocumentService(): TextDocumentService = textDocumentService

    override fun getWorkspaceService(): WorkspaceService = workspaceService
}