package net.jkcode.jkmvc.iterator

import net.jkcode.jkmvc.elements.ElementArray
import net.jkcode.jkmvc.elements.IElements

/**
 * 对数组进行有过滤条件有转换元素的迭代
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-21 10:23 AM
 */
abstract class ArrayFilteredTransformedIterator<E, R>(arr: Array<E> /* 目标数组 */):
        ElementsFilteredTransformedIterator<E, R>(),
        IElements<E> by ElementArray(arr) { // 通过代理 ElementArray 来实现 IElements
}