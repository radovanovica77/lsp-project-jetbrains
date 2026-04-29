package com.logolsp.analysis

class DocumentManager {
    private val documents = mutableMapOf<String, String>()

    fun update(uri: String, content: String) {
        documents[uri] = content
    }

    fun get(uri: String): String? = documents[uri]

    fun remove(uri: String) {
        documents.remove(uri)
    }
}