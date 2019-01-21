package com.jkmvc.tests.model

import com.jkmvc.orm.Orm
import com.jkmvc.orm.OrmMeta

/**
 * 包裹模型
 *
 * @ClassName: ParcelModel
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2017-11-24 09:42:34
 */
class ParcelModel(id:Int? = null): Orm(id) {
    // 伴随对象就是元数据
    companion object m: OrmMeta(ParcelModel::class, "包裹模型", "parcel", "id"){

        init {
            // 添加标签 + 规则
            // add label and rule for field
            addRule("sender_id", "寄件人", "notEmpty");
            addRule("receiver_id", "收件人", "notEmpty");
            addRule("content", "内容", "notEmpty");

            // 添加关联关系
            // add relaction for other model
            belongsTo("sender", UserModel::class, "sender_id") // 寄件人
            belongsTo("receiver", UserModel::class, "receiver_id") // 收件人
        }

    }

    // 代理属性读写
    public var id:Int by property() // 包裹id

    public var senderId:Int by property() // 寄件人id

    public var receiverId:Int by property() // 收件人id

    public var content:String by property() // 寄件内容

}