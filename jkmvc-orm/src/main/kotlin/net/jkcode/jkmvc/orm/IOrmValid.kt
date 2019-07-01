package net.jkcode.jkmvc.orm

/**
 * ORM之数据校验
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
interface IOrmValid : IOrmEntity {

    /**
     * 校验数据
     * @return
     */
    fun validate(): Boolean;

    /**
     * 改写 toString()
     *   在实体类 XXXEntity 与模型类 XXXModel 分离的场景下改写 OrmEntity.toString(), 如:
     *   XXXEntity: open class MessageEntity: OrmEntity()
     *   XXXModel: class MessageModel: MessageEntity(), IOrm by GeneralModel(m)
     *   而 XXXModel 继承于 XXXEntity 是为了继承与复用其声明的属性, 但是 IOrm 的方法全部交由 GeneralModel 代理来改写, 也就对应改写掉 XXXEntity/OrmEntity 中与 IOrm 重合的方法(即 IOrmEntity 的方法)
     *   但是某些方法与属性是 XXXEntity/OrmEntity 特有的, 没有归入 IOrm 接口, 也就是说 GeneralModel 不能改写这些方法与属性
     *   => 将 toString() 归入 IOrm 接口
     */
    override fun toString(): String

}
