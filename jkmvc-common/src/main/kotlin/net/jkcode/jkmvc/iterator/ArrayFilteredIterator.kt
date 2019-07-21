package net.jkcode.jkmvc.iterator

/**
 * 对数组进行有过滤条件的迭代
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-21 10:23 AM
 */
abstract class ArrayFilteredIterator<E>(arr: Array<E>): ArrayFilteredTransformedIterator<E, E>(arr){
    /**
     * 迭代元素的转换
     */
    public override fun transform(ele: E, i: Int): E {
        return ele
    }
}
