package net.jkcode.jkmvc.iterator

import net.jkcode.jkmvc.common.joinToString
import net.jkcode.jkmvc.elements.IElements
import java.util.NoSuchElementException

/**
 * 对多个元素进行有过滤条件有转换元素的迭代
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-21 10:23 AM
 */
abstract class ElementsFilteredTransformedIterator<E, R>: Iterator<R>, IElements<E> {

    /**
     * 下一序号
     */
    // protected var _next = prepareNext(-1) // 子类通过代理实现了 IElements<E>, 而代理只在子类构造函数中赋值, 在当前类构造函数之(即_next初始化)之后, 因此你在这里调用涉及到代理的 prepareNext() 方法, 只会报 NullPointerException 异常
    //protected val _next:Int by lazy { prepareNext(-1) } // 递延初始化, 但只能为 val, 不能记录下一个
    protected var _next = -1 // 递延初始化 + 变更值含义: -1 初始值 -2 没有下一个

    /**
     * 过滤元素
     */
    abstract fun filter(ele: E): Boolean

    /**
     * 迭代元素的转换
     */
    abstract fun transform(ele: E, i: Int): R

    /**
     * 初始化下一序号
     */
    protected fun initNext(){
        if(_next == -1)
            _next = prepareNext(-1)
    }

    /**
     * 准备下一序号
     * @param start 开始序号
     * @return
     */
    protected fun prepareNext(start: Int): Int {
        var i = start
        while (++i < size()) {
            if (filter(getElement(i)))
                return i
        }
        return -2
    }

    public override fun hasNext(): Boolean {
        initNext()
        return _next != -2;
    }

    public override fun next(): R {
        initNext()
        if (_next == -2)
            throw NoSuchElementException();

        var curr = _next
        _next = prepareNext(_next) // 准备下一序号
        return transform(getElement(curr), curr)
    }

    public override fun toString(): String {
        return this.joinToString(", ","ElementsFilteredTransformedIterator(", ")")
    }

}