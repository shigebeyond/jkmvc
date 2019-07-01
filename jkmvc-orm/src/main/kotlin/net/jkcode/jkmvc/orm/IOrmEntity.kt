package net.jkcode.jkmvc.orm

/**
 * ORM之实体对象
 *  1. 本来想继承 MutableMap<String, Any?>, 但是得不偿失, 不值得做
 *    仅仅需要的是get()/put()
 *    可有可无的是size()/isEmpty()/containsKey()/containsValue()
 *    完全不需要的是remove()/clear()/keys/values/entries/MutableEntry
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
interface IOrmEntity {

    /**
     * 判断是否有某字段
     *
     * @param column
     * @return
     */
    fun hasColumn(column: String): Boolean;

    /**
     * 获得对象字段
     *
     * @param column 字段名
     * @param defaultValue 默认值
     * @return
     */
    operator fun <T> get(column: String, defaultValue: T? = null): T;

    /**
     * 获得或设置字段值
     *
     * @param key
     * @param defaultValue
     * @return
     */
    fun getOrPut(key: String, defaultValue: () -> Any?): Any? {
        // 获得字段值
        val value: Any? = get(key)
        if (value != null)
            return value

        // 设置字段值
        val answer = defaultValue()
        set(key, answer)
        return answer
    }

    /**
     * 设置对象字段值
     *
     * @param column 字段名
     * @param  value  字段值
     */
    operator fun set(column: String, value: Any?);

    /**
     * 智能设置属性
     *    在不知属性类型的情况下，将string赋值给属性
     *    => 需要将string转换为属性类型
     *    => 需要显式声明属性
     *
     * <code>
     *     class UserModel(id:Int? = null): Orm(id) {
     *          ...
     *          public var id:Int by property<Int>(); //需要显式声明属性
     *     }
     *
     *     val user = UserModel()
     *     user.id = String.parseInt("123")
     *     // 相当于
     *     user.setIntelligent("id", "123")
     * </code>
     *
     * @param column
     * @param value 字符串
     * @return
     */
    fun setIntelligent(column:String, value:String):Boolean

    /**
     * 从map中设置字段值
     *
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param expected 要设置的字段名的列表
     */
    fun fromMap(from: Map<String, Any?>, expected: List<String> = emptyList()): Unit;

    /**
     * 获得字段值 -- 转为Map
     * @param to
     * @param expected 要设置的字段名的列表
     * @return
     */
    fun toMap(to: MutableMap<String, Any?> = HashMap(), expected: List<String> = emptyList()): MutableMap<String, Any?>;

    /**
     * 从其他实体对象中设置字段值
     *
     * @param from
     */
    fun from(from: IOrmEntity): Unit

    /**
     * 序列化
     * @return
     */
    fun serialize(): ByteArray?

    /**
     * 序列化
     *
     * @param bytes
     */
    fun unserialize(bytes: ByteArray): Unit

    /**
     * 编译字符串模板
     *
     * @param template 字符串模板，格式 "name=:name, age=:age"，用:来修饰字段名
     * @return 将模板中的字段名替换为字段值
     */
    fun compileTemplate(template:String):String

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