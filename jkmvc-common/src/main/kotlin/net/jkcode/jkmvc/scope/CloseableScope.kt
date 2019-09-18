package net.jkcode.jkmvc.scope

import java.io.Closeable

/**
 * 将 Closeable 转为 IScope
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-18 9:24 AM
 */
class CloseableScope(val closing: Closeable): IScope {

    /**
     * 添加子作用域
     * @param childScope
     */
    public override fun addChildScope(childScope: IScope) {
        throw UnsupportedOperationException()
    }

    /**
     * 作用域开始
     */
    public override fun beginScope() {
    }

    /**
     * 作用域结束
     */
    public override fun endScope() {
        closing.close()
    }


}