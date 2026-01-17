package io.murphy.core

data class MurphyResponse(
    val code: Int,
    val body: ByteArray,
    val headers: Map<String, List<String>>,
) {

    companion object {
        @JvmStatic
        @JvmOverloads
        fun of(
            code: Int,
            body: ByteArray = ByteArray(0),
            headers: Map<String, String> = emptyMap(),
        ): MurphyResponse {
            return MurphyResponse(
                code = code,
                body = body,
                headers = headers.mapValues { listOf(it.value) }
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MurphyResponse

        if (code != other.code) return false
        if (!body.contentEquals(other.body)) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code
        result = 31 * result + body.contentHashCode()
        result = 31 * result + headers.hashCode()
        return result
    }
}
