package com.logolsp.features

import com.logolsp.analysis.AnalysisResult
import com.logolsp.analysis.SymbolKind
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

class DefinitionProvider {

    fun provide(
        result: AnalysisResult,
        uri: String,
        line: Int,
        character: Int
    ): Location? {
        // Nađi referencu na kojoj je kursor
        val ref = result.references.firstOrNull { ref ->
            ref.line == line &&
                    character >= ref.column &&
                    character <= ref.column + ref.name.length
        } ?: return null

        // Nađi definiciju — mora da se poklapa i ime I tip (procedura vs varijabla)
        val symbol = result.symbols.firstOrNull {
            it.name == ref.name.uppercase() && it.kind == ref.kind
        } ?: return null

        val startPos = Position(symbol.line, symbol.column)
        val endPos = Position(symbol.line, symbol.column + symbol.name.length)
        return Location(symbol.uri, Range(startPos, endPos))
    }
}