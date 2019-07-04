package net.jkcode.jkmvc.common

/**
 * 可修改的pair
 */
public data class MutablePair<A, B>(
        public var first: A,
        public var second: B
) {
    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    public override fun toString(): String = "($first, $second)"
}

/**
 * 可修改的Triple
 */
public data class MutableTriple<A, B, C>(
        public var first: A,
        public var second: B,
        public var third: C
) {

    /**
     * Returns string representation of the [Triple] including its [first], [second] and [third] values.
     */
    public override fun toString(): String = "($first, $second, $third)"
}