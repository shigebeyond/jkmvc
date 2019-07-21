package net.jkcode.jkmvc.bit;

import net.jkcode.jkmvc.elements.IElements;

import java.util.BitSet;
import java.util.Iterator;

/**
 * 空比特对应元素的迭代器
 *
 * @author shijianhang
 * @date 2019-06-27 11:58 AM
 */
public class ClearBitElementIterator<E> implements Iterator<E> {

    protected BitSet bits;

    protected IElements<E> op;

    protected int curr = -1;

    public ClearBitElementIterator(BitSet bits, IElements<E> op) {
        this.bits = bits;
        this.op = op;
    }

    @Override
    public boolean hasNext() {
        return bits.nextClearBit(curr + 1) >= 0;
    }

    @Override
    public E next() {
        curr = bits.nextClearBit(curr + 1);
        return op.getElement(curr);
    }

    @Override
    public void remove() {
        if (curr != -1) {
            op.removeElement(curr); // 删除元素
            bits.set(curr); // 设置位
        }

    }
}