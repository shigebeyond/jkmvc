package net.jkcode.jkmvc.bit;

import net.jkcode.jkmvc.elements.IElements;

import java.util.BitSet;
import java.util.Iterator;

/**
 * 比特对应元素的迭代器
 *
 * @author shijianhang
 * @date 2019-06-27 11:58 AM
 */
public class SetBitElementIterator<E> implements Iterator<E> {

    protected BitSet bits;

    protected IElements<E> op;

    protected int curr = -1;

    public SetBitElementIterator(BitSet bits, IElements<E> op) {
        this.bits = bits;
        this.op = op;
    }

    @Override
    public boolean hasNext() {
        return bits.nextSetBit(curr + 1) >= 0;
    }

    @Override
    public E next() {
        curr = bits.nextSetBit(curr + 1);
        return op.getElement(curr);
    }

    @Override
    public void remove() {
        if (curr != -1) {
            op.removeElement(curr); // 删除元素
            bits.clear(curr); // 清空比特 -- 在 FixedKeyMap.remove(int) 实现中也调用了这行, 从而导致重复执行, 无所谓
        }
    }
}
