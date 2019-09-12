package net.jkcode.jkmvc.tests.model

import net.jkcode.jkmvc.model.GeneralModel
import net.jkcode.jkmvc.orm.IEntitiableOrm
import net.jkcode.jkmvc.orm.IOrm
import net.jkcode.jkmvc.orm.OrmMeta
import net.jkcode.jkmvc.tests.entity.MessageEntity

/**
 * 消息模型
 *    实体类与模型类分离
 *    实体类直接继承 OrmEntity, 不继承 IOrm 或 Orm
 *    模型类直接继承 实体类, 同时继承 IOrm, 不直接继承 Orm
 *
 * @ClassName: MessageModel
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-27 14:51:51
 */
class MessageModel: MessageEntity(), IOrm by GeneralModel(m), IEntitiableOrm<MessageEntity> {

	// 伴随对象就是元数据
 	companion object m: OrmMeta(MessageModel::class, "消息", "message", "id"){}

}