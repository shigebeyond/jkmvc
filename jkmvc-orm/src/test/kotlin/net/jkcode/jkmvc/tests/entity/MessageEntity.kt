package net.jkcode.jkmvc.tests.entity

import net.jkcode.jkmvc.es.annotation.EsDoc
import net.jkcode.jkmvc.es.annotation.EsId
import net.jkcode.jkmvc.orm.OrmEntity

/**
 * 消息实体
 *    实体类与模型类分离
 *    实体类直接继承 OrmEntity, 不继承 IOrm 或 Orm
 *    模型类直接继承 实体类, 同时继承 IOrm, 不直接继承 Orm
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-27 2:53 PM
 */
@EsDoc("message_index", "_doc")
open class MessageEntity: OrmEntity() {

    // 代理属性读写
    @EsId
    public var id:Int by property() // 消息id

    public var fromUid:Int by property() // 发送人id

    public var toUid:Int by property() // 接收人id

    public var created:Long by property() // 接收人id

    public var content:String by property() // 消息内容

    override fun toString(): String {
        return "MessageEntity(" + toMap() + ")"
    }


}