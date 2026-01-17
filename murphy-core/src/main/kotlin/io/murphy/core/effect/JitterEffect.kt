package io.murphy.core.effect

import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse
import java.util.concurrent.ThreadLocalRandom

internal class JitterEffect(
    private val min: Long,
    private val max: Long,
) : DelayEffect() {

    override val duration: Long
        get() = if (min >= max) min else ThreadLocalRandom.current().nextLong(min, max + 1)

    override fun apply(context: MurphyContext): MurphyResponse? {
        val delay = duration
        if (delay > 0) {
            Thread.sleep(delay)
        }
        return null
    }
}
