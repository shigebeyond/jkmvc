package net.jkcode.jkmvc.tests.model

import net.jkcode.jkmvc.model.GeneralModel
import net.jkcode.jkmvc.orm.IOrm
import net.jkcode.jkmvc.orm.OrmMeta
import net.jkcode.jkmvc.tests.entity.Message

/**
 * 消息
 *
 * @ClassName: MessageModel
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2019-06-27 14:51:51
 */
class MessageModel(): Message(), IOrm by GeneralModel(m) {

	// 伴随对象就是元数据
 	companion object m: OrmMeta(MessageModel::class, "消息", "message", "id"){}

}