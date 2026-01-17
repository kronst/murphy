package io.murphy.core.effect

import io.murphy.core.Effect
import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse
import java.util.concurrent.ThreadLocalRandom

internal class ProbabilisticEffect(
    private val delegate: Effect,
    private val probability: Double,
) : Effect {

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
