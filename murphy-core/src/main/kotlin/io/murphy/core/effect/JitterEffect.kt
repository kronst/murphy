package io.murphy.core.effect

import java.util.concurrent.ThreadLocalRandom

internal class JitterEffect(
    private val min: Long,
    private val max: Long,
) : DelayEffect() {

    override val duration: Long
        get() = if (min >= max) min else ThreadLocalRandom.current().nextLong(min, max + 1)
}
