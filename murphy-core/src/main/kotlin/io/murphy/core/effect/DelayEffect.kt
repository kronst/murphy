package io.murphy.core.effect

import io.murphy.core.Effect
import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse

sealed class DelayEffect : Effect {
    abstract val duration: Long

    override fun apply(context: MurphyContext): MurphyResponse? {
        val d = duration
        if (d > 0) {
            Thread.sleep(d)
        }
        return null
    }
}
