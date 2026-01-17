package io.murphy.core.effect

import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse

internal class LatencyEffect(override val duration: Long) : DelayEffect() {

    override fun apply(context: MurphyContext): MurphyResponse? {
        if (duration > 0) {
            Thread.sleep(duration)
        }
        return null
    }
}
