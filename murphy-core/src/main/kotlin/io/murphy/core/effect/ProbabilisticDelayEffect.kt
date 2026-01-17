package io.murphy.core.effect

import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse
import java.util.concurrent.ThreadLocalRandom

internal class ProbabilisticDelayEffect(
    private val delegate: DelayEffect,
    private val probability: Double,
) : DelayEffect() {

    override val duration: Long
        get() = delegate.duration

    override fun probability(): Double = probability

    override fun apply(context: MurphyContext): MurphyResponse? {
        val roll = ThreadLocalRandom.current().nextDouble()

        return if (roll <= probability) {
            delegate.apply(context)
        } else {
            null
        }
    }
}
