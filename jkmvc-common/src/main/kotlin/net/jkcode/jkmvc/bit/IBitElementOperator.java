package net.jkcode.jkmvc.bit;

/**
 * 比特对应元素的操作器
 * @author shijianhang
 * @date 2019-06-27 12:00 PM
 */
public interface IBitElementOperator<E> {
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

    /**
     * 获得元素个数
     * @return
     */
    int getElementSize();
}