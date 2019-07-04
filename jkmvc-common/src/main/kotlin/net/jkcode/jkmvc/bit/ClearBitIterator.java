package net.jkcode.jkmvc.bit;

import java.util.BitSet;
import java.util.Iterator;

/**
 * 空比特的迭代器
 * @author shijianhang
 * @date 2019-06-27 11:59 AM
 */
public class ClearBitIterator implements Iterator<Integer> {

    protected BitSet bits;

    protected int curr = -1;

    public ClearBitIterator(BitSet bits) {
        this.bits = bits;
    }

    @Override
    public boolean hasNext() {
        return bits.nextClearBit(curr + 1) >= 0;
    }

    @Override
    public Integer next() {
        return curr = bits.nextClearBit(curr + 1);
    }

    @Override
    public void remove() {
        if (curr != -1)
            bits.set(curr);
    }
}