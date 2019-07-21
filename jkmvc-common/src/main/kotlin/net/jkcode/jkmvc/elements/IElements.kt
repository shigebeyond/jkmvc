package net.jkcode.jkmvc.elements

/**
 * 多个元素的抽象
 * @author shijianhang
 * @date 2019-06-27 12:00 PM
 */
interface IElements<E> {

    /**
     * 获得元素个数
     * @return
     */
    fun size(): Int

    /**
     * 获得元素
     *
     * @param index
     * @return
     */
    fun getElement(index: Int): E

    /**
     * 删除元素
     *
     * @param index
     * @return
     */
    fun removeElement(index: Int): Boolean {
        throw UnsupportedOperationException()
    }
}