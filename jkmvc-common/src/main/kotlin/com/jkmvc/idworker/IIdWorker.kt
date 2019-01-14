package com.jkmvc.idworker

import com.jkmvc.common.Config
import com.jkmvc.common.IConfig
import com.jkmvc.common.NamedSingleton

/**
 * id生成器
 * @author shijianhang
 * @date 2017-10-8 下午8:02:47
 */
interface IIdWorker {

    // 可配置的单例
    companion object: NamedSingleton<IIdWorker>() {
        /**
         * 单例类的配置，内容是哈希 <单例名 to 单例类>
         */
        public override val instsConfig: IConfig = Config.instance("idworker", "yaml")
    }

    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return
     */
    fun nextId(): Long
}
