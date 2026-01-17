package io.murphy.core

import io.murphy.core.effect.JitterEffect
import io.murphy.core.effect.LatencyEffect
import java.io.IOException
import java.util.concurrent.TimeUnit

object Effects {

    @JvmStatic
    @JvmOverloads
    fun latency(duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Effect {
        return LatencyEffect(duration = unit.toMillis(duration))
    }

    @JvmStatic
    @JvmOverloads
    fun jitter(min: Long, max: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Effect {
        return JitterEffect(min = unit.toMillis(min), max = unit.toMillis(max))
    }

    @JvmStatic
    @JvmOverloads
    fun response(
        code: Int = 200,
        body: ByteArray = ByteArray(0),
        headers: Map<String, List<String>> = emptyMap(),
    ): Effect {
        return Effect {
            MurphyResponse(code = code, body = body, headers = headers)
        }
    }

    @JvmStatic
    fun status(code: Int): Effect {
        return Effect {
            MurphyResponse(code = code, body = ByteArray(0), headers = emptyMap())
        }
    }

    @JvmStatic
    @JvmOverloads
    fun json(code: Int = 200, body: String): Effect {
        return Effect {
            MurphyResponse(
                code = code,
                body = body.toByteArray(Charsets.UTF_8),
                headers = mapOf("Content-Type" to listOf("application/json")),
            )
        }
    }

    @JvmStatic
    @JvmOverloads
    fun crash(message: String? = null): Effect {
        return Effect {
            throw IOException(message ?: "Murphy induced crash")
        }
    }

    /**
     * Prefer throwing [IOException] subclasses to better simulate network errors.
     */
    @JvmStatic
    fun crash(exception: Exception): Effect {
        return Effect {
            throw exception
        }
    }
}
