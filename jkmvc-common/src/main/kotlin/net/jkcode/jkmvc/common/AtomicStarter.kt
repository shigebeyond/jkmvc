package net.jkcode.jkmvc.common

import java.util.concurrent.atomic.AtomicBoolean

/**
 * 使用AtomicBoolean实现一次性的启动
 * @author shijianhang<772910474@qq.com>
 * @date 2019-07-17 6:45 PM
 */
class OnceStarter {

    /**
     * 是否已启动
     */
    protected val start: AtomicBoolean = AtomicBoolean(false)

    /**
     * 启动
     * @param block 处理
     * @return
     */
    public inline fun start(block: () -> Unit): Boolean {
        val started = start.compareAndSet(false, true)
        if(started){
            block()
        }
        return started

    }

}