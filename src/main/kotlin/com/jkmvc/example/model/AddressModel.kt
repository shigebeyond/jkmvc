package com.jkmvc.example.model

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm

/**
 * 地址模型
 */
class AddressModel(id:Int? = null): Orm(id) {
    // 伴随用户就是元数据
    companion object m: MetaData(AddressModel::class){
        init {
            belongsTo("user", UserModel::class, "user_id")
        }

        public override val rules: MutableMap<String, String> = mutableMapOf(
                "tel" to "digit"
        );

        /**
         * 每个字段的标签（中文名）
         */
        public override val labels: MutableMap<String, String>  = mutableMapOf(
                "user_id" to "用户",
                "addr" to "地址",
                "tel" to "电话"
        );
    }

    public var user_id:Int by property<Int>();

    public var addr:String by property<String>();

    public var tel:String by property<String>();

    // 关联用户：一个地址从属于一个用户
    public var user:UserModel by property<UserModel>()
}