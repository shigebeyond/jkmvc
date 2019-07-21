package net.jkcode.jkmvc.common

import net.jkcode.jkmvc.bit.SetBitIterator
import java.util.*

/**
 * 获得比特集的迭代器
 * @return
 */
public fun BitSet.iterator(): SetBitIterator {
    return SetBitIterator(this)
}

/**
 * Performs the given [operation] on each element of this [SetBitIterator].
 * @sample samples.collections.Iterators.forEachIterator
 */
public inline fun BitSet.forEach(action: (Int) -> Unit): Unit {
    iterator().forEach(action)
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
public inline fun <R> BitSet.map(transform: (Int) -> R): List<R> {
    return iterator().map(transform)
}