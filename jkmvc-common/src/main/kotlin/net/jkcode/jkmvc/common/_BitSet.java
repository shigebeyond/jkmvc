package net.jkcode.jkmvc.common;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Iterator;

/**
 * BitSet相关集合
 */
abstract class IBitCollection<E> extends AbstractCollection<E> implements IBitElementOperator<E> {

    /**
     * BitSet相关集合操作助手
     */
    protected BitCollectionHelper<E> helper;

    public IBitCollection(BitSet bits) {
        helper = new BitCollectionHelper(bits, this);
    }

    /**
     * 获得BitSet中设置为 true 的位数
     * @return
     */
    @Override
    public int size() {
        return helper.size();

    }

    /**
     * 获得迭代器
     * @return
     */
    @Override
    public Iterator<E> iterator() {
        return helper.iterator();
    }
};

/**
 * BitSet相关集合
 */
abstract class IBitSet<E> extends AbstractSet<E> implements IBitElementOperator<E> {
    /**
     * BitSet相关集合操作助手
     */
    protected BitCollectionHelper<E> helper;

    public IBitSet(BitSet bits) {
        helper = new BitCollectionHelper(bits, this);
    }

    /**
     * 获得BitSet中设置为 true 的位数
     * @return
     */
    @Override
    public int size() {
        return helper.size();
    }

    /**
     * 获得迭代器
     * @return
     */
    @Override
    public Iterator<E> iterator() {
        return helper.iterator();
    }
}

/**
 * 比特对应元素的操作器
 */
interface IBitElementOperator<E> {
    /**
     * 获得比特位对应的元素
     *
     * @param index
     * @return
     */
    E getElement(int index);

    /**
     * 删除比特位对应的元素
     *
     * @param index
     * @return
     */
    boolean removeElement(int index);
}

/**
 * BitSet相关集合操作助手
 */
class BitCollectionHelper<E> {

    protected BitSet bits;

    protected IBitElementOperator<E> op;

    BitCollectionHelper(BitSet bits, IBitElementOperator<E> op) {
        this.bits = bits;
        this.op = op;
    }

    /**
     * 获得BitSet中设置为true的位数
     * @return
     */
    public int size() {
        return bits.cardinality();
    }

    /**
     * 获得元素迭代器
     * @return
     */
    public Iterator<E> iterator() {
        return new BitElementIterator();
    }

    /**
     * 比特对应元素的迭代器
     */
    public class BitElementIterator implements Iterator<E> {

        protected int curr = -1;

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
            if (curr != -1)
                op.removeElement(curr);
        }
    }
};





