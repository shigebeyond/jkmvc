package net.jkcode.jkmvc.bit;

import net.jkcode.jkmvc.elements.IElements;

import java.util.AbstractCollection;
import java.util.BitSet;
import java.util.Iterator;

/**
 * BitSet相关集合
 *   直接根据 BitSet 来修改 IBitCollection 的语义
 *   可用于实现 FixedKeyMap 的 Values
 *
 * @author shijianhang
 * @date 2019-06-27 11:57 AM
 */
public abstract class IBitCollection<E> extends AbstractCollection<E> implements IElements<E> {

    protected BitSet bits;

    public IBitCollection(BitSet bits) {
        this.bits = bits;
    }

    /**
     * 获得BitSet中设置为 true 的位数
     * @return
     */
    @Override
    public int size() {
        return bits.cardinality();
    }

    /**
     * 获得迭代器
     * @return
     */
    @Override
    public Iterator<E> iterator() {
        return new SetBitElementIterator(bits,this);
    }
};