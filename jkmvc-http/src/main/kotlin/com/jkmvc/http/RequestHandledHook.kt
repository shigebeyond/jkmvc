package com.jkmvc.common

import com.jkmvc.db.Db

/**
 * 请求处理后事件的钩子，用于关闭资源
 *   在 RequestHandler::handle() 中调用
 *
 * @ClassName: RequestHandledHook
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-12-16 3:48 PM
 */
object RequestHandledHook : ClosingHook() {

    init {
        // 关闭当前线程的所有db
        addClosings(Db.all())
    }
}