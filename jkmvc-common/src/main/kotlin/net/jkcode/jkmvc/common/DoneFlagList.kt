package net.jkcode.jkmvc.common

import net.jkcode.jkmvc.iterator.CollectionFilteredTransformedIterator
import java.util.*

/**
 * 标记元素已完成的列表
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-04 11:29 AM
 */
class DoneFlagList<T>(val list: MutableList<MutablePair<T, Boolean>> = LinkedList()): AbstractMutableList<T>() {

    public override val size: Int
        get() = list.size

    public override fun get(index: Int): T {
        return list[index].first
    }

    public override fun add(index: Int, element: T) {
        list.add(index, MutablePair(element, false))
    }

    public override fun removeAt(index: Int): T {
        return list.removeAt(index).first
    }

    public override fun set(index: Int, element: T): T {
        return list.set(index, MutablePair(element, false)).first
    }

    /**
     * 设置元素是否已完成的标志
     * @param index 元素下标
     * @param value 是否已完成
     */
    public fun setDone(index: Int, value: Boolean = true){
        list[index].second = value
    }

    /**
     * 检查元素是否已完成
     * @param index 元素下标
     * @return 是否已完成
     */
    public fun isDone(index: Int): Boolean {
        return list[index].second
    }

    /**
     * 检查全部元素是否已完成
     * @return
     */
    public fun isAllDone(): Boolean {
        return list.all { it.second }
    }

    /**
     * 获得已完成/未完成元素的迭代器
     * @param done 迭代已完成, 还是未完成?
     * @return
     */
    public fun doneIterator(done: Boolean): Iterator<Pair<Int, T>> {
        return DoneIterator(done)
    }

    /**
     * 已完成/未完成元素的迭代器
     */
    protected inner class DoneIterator(protected val done: Boolean /* 是否已完成 */) : CollectionFilteredTransformedIterator<MutablePair<T, Boolean>, Pair<Int, T>>(list){
        override fun filter(ele: MutablePair<T, Boolean>): Boolean {
            return ele.second == done // done
        }

        override fun transform(ele: MutablePair<T, Boolean>, i: Int): Pair<Int, T> {
            return i to ele.first
        }

    }

}