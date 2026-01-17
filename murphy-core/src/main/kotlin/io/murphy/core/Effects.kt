package io.murphy.core

import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

object Effects {

    @JvmStatic
    @JvmOverloads
    fun latency(value: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Effect {
        return Effect {
            Thread.sleep(unit.toMillis(value))
            null
        }
    }

    @JvmStatic
    @JvmOverloads
    fun jitter(min: Long, max: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Effect {
        val minMillis = unit.toMillis(min)
        val maxMillis = unit.toMillis(max)

        return Effect {
            val delay = ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1)
            Thread.sleep(delay)
            null
        }
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
    fun json(code: Int, body: String): Effect {
        return Effect {
            MurphyResponse(
                code = code,
                body = body.toByteArray(Charsets.UTF_8),
                headers = mapOf("Content-Type" to listOf("application/json")),
            )
        }
    }

    @JvmStatic
    fun json(body: String): Effect {
        return json(code = 200, body = body)
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
