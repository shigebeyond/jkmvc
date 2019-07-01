package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.common.LazyAllocatedMap
import net.jkcode.jkmvc.db.MutableRow

/**
 * ORM之数据校验
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmValid : IOrm, OrmEntity() {

    /**
     * 改写 OrmEntity 中的 data属性
     * 最新的字段值：<字段名 to 最新字段值>
     */
    protected override val data: MutableRow by lazy{
        try {
            ormMeta.dataFactory.createMap()
        }catch (e: Exception){
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 变化的字段值：<字段名 to 原始字段值>
     *     一般只读，lazy创建，节省内存
     */
    protected val dirty: MutableRow = LazyAllocatedMap<String, Any?>()

    /**
     * 设置对象字段值
     *    支持记录变化的字段名 + 原始值
     *
     * @param column 字段名
     * @param  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        if (!hasColumn(column))
            throw OrmException("类 ${this.javaClass} 没有字段 $column");

        // 记录变化的字段名 + 原始值
        if(!dirty.containsKey(column)
                //&& value != data[column])
                && !equalsValue(data[column], value))
            dirty[column] = data[column];

        super.set(column, value)
    }

    /**
     * 校验数据
     * @return
     */
    public override fun validate(): Boolean {
        return ormMeta.validate(this)
    }

    /**
     * 改写 toString()
     *   在实体类 XXXEntity 与模型类 XXXModel 分离的场景下改写 OrmEntity.toString(), 如:
     *   XXXEntity: open class MessageEntity: OrmEntity()
     *   XXXModel: class MessageModel: MessageEntity(), IOrm by GeneralModel(m)
     *   而 XXXModel 继承于 XXXEntity 是为了继承与复用其声明的属性, 但是 IOrm 的方法全部交由 GeneralModel 代理来改写, 也就对应改写掉 XXXEntity/OrmEntity 中与 IOrm 重合的方法(即 IOrmEntity 的方法)
     *   但是某些方法与属性是 XXXEntity/OrmEntity 特有的, 没有归入 IOrm 接口, 也就是说 GeneralModel 不能改写这些方法与属性
     *   => 将 toString() 归入 IOrm 接口
     */
    public override fun toString(): String {
        return "${this.javaClass}: $data"
    }
}
