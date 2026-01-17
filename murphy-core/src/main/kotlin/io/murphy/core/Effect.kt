package io.murphy.core

fun interface Effect {
    fun apply(context: MurphyContext): MurphyResponse?
}
