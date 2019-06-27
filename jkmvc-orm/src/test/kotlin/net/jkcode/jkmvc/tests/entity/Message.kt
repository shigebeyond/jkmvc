package net.jkcode.jkmvc.tests.entity

import net.jkcode.jkmvc.orm.OrmEntity

/**
 * 消息实体
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-27 2:53 PM
 */
open class Message: OrmEntity() {

    // 代理属性读写
    public var id:Int by property() // 消息id

    public var fromUid:Int by property() // 发送人id

    public var toUid:Int by property() // 接收人id

    public var content:String by property() // 消息内容
}