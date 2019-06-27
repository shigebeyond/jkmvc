package net.jkcode.jkmvc.common

import java.util.*

/**
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-26 7:18 PM
 */
class BitIndexIterator(protected val bits:BitSet): Iterator<Int> {

    protected var pos = 0

    override fun hasNext(): Boolean {
        bits.nextSetBit(pos)
    }

    override fun next(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}