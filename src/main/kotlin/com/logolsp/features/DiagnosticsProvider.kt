package com.logolsp.features

import com.logolsp.analysis.AnalysisResult
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

class DiagnosticsProvider {

    fun provide(result: AnalysisResult): List<Diagnostic> {
        return result.diagnostics.map { d ->
            Diagnostic(
                Range(
                    Position(d.line, d.column),
                    Position(d.line, d.column + d.length)
                ),
                d.message,
                DiagnosticSeverity.Error,
                "logo-lsp"
            )
        }
    }
}