package io.murphy.core.effect

import java.util.concurrent.ThreadLocalRandom

internal class ProbabilisticDelayEffect(
    private val delegate: DelayEffect,
    private val probability: Double,
) : DelayEffect() {

    override val duration: Long
        get() {
            val roll = ThreadLocalRandom.current().nextDouble()
            return if (roll <= probability) delegate.duration else 0L
        }

    override fun probability(): Double = probability
}
