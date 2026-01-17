package io.murphy.core.effect

import io.murphy.core.Effect

sealed class DelayEffect : Effect {
    abstract val duration: Long
}
