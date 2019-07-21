package net.jkcode.jkmvc.iterator

/**
 * 对集合进行有过滤条件的迭代
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-21 10:23 AM
 */
abstract class CollectionFilteredIterator<E>(col: Collection<E>): CollectionFilteredTransformedIterator<E, E>(col){
    /**
     * 迭代元素的转换
     */
    public override fun transform(ele: E, i: Int): E {
        return ele
    }
}
