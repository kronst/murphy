package io.murphy.core

fun interface Effect {
    fun apply(context: MurphyContext): MurphyResponse?
    fun probability(): Double = 1.0
}
