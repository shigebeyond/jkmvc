package com.jkmvc.session

import com.jkmvc.orm.MetaData
import com.jkmvc.orm.Orm

/**
 * 会话相关的用户模型
 *    该类是默认实现
 *    可自己实现，需修改 session.properties 指定 userModel
 *
 * @author shijianhang
 * @create 2017-09-19 下午11:35
 */
class UserModel(id:Int? = null): Orm(id) {
    // 伴随对象就是元数据
    companion object m: MetaData(UserModel::class){
        init {
            // 添加标签 + 规则
            addRule("username", "用户名", "notEmpty");
            addRule("password", "密码", "notEmpty");
        }
    }

    // 代理属性读写
    public var id:Int by property<Int>();

    public var username:String by property<String>();

    public var password:String by property<String>();
}
