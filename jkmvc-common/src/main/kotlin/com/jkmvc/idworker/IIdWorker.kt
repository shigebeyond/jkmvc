package net.jkcode.jkmvc.idworker

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.IConfig
import net.jkcode.jkmvc.singleton.NamedConfiguredSingletons

/**
 * id生成器
 * @author shijianhang
 * @date 2017-10-8 下午8:02:47
 */
interface IIdWorker {

    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return
     */
    fun nextId(): Long
}
