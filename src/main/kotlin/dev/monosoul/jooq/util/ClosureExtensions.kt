package dev.monosoul.jooq.util

import groovy.lang.Closure

internal fun <T> Closure<T>.callWith(target: T) = rehydrate(target, target, target).call(target)
