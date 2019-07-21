package net.jkcode.jkmvc.elements

/**
 * 元素数组
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-21 10:58 AM
 */
class ElementArray<E>(protected val arr: Array<E>): IElements<E> {

    public override fun size(): Int {
        return arr.size
    }

    public override fun getElement(index: Int): E {
        return arr[index]
    }

}