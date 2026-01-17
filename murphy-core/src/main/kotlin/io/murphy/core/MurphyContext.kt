package io.murphy.core

data class MurphyContext(
    val url: String,
    val path: String,
    val method: String,
    val headers: Map<String, List<String>>,
)
