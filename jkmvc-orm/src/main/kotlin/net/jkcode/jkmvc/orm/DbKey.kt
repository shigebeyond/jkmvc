package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.toArray
import net.jkcode.jkmvc.query.IDbQueryBuilder
import net.jkcode.jkutil.common.isSame
import java.util.*

/**
 * 复合主键, 包含多个字段
 * 1 CharSequence接口
 *   为了适配 DbQueryBuilderDecoration 中的条件方法的参数类型, 如 where()
 *   否则要重载很多方法来接收 DbKeyName 参数
 *
 * 2 data class
 *   不用手动实现 equals() 方法
 *
 * 3 访问权限
 *   原来想限制类的访问权限为 internal, 但是 IOrmMeta 与 IRelationMeta 的主键外键属性都>直接暴露了该类
 *   那只好限制扩展方法了
 *
 * 4 复合主键中字段值不能为null
 *   All parts of a PRIMARY KEY must be NOT NULL
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2018-12-17 7:13 PM
 */
class DbKey<T> {

    companion object {
        /**
         * 空主键
         */
        internal val empty:DbKey<*> = DbKey<Any>()
    }

    public lateinit var columns: Array<T>

    public constructor(vararg columns: T){
        this.columns = columns as Array<T>
    }
    /**
     * 字段个数
     */
    public val size
        get() = columns.size

    /**
     * 第一个字段
     */
    public fun first(): T {
        return columns.first()
    }

    /**
     * 唯一一个字段
     */
    public fun single(): T {
        return columns.single()
    }

    /**
     * 遍历并生成新的主键
     *
     * @param transform 字段转换函数
     * @return
     */
    public inline fun <R> map(transform: (T) -> R): DbKey<R> {
        val newKeys = arrayOfNulls<Any>(columns.size)
        forEachColumn { i, v ->
            newKeys[i] = transform(columns[i])
        }
        return DbKey(*newKeys) as DbKey<R>
    }

    /**
     * 与另外一个主键 遍历并生成新的主键
     *
     * @param transform 字段转换函数
     * @return
     */
    public inline fun <S, R> mapWith(other: DbKey<S>, transform: (T, S) -> R): DbKey<R> {
        val newKeys = arrayOfNulls<Any>(columns.size)
        forEachColumnWith(other){ col1, col2, i ->
            newKeys[i] = transform(col1, col2)
        }
        return DbKey(*newKeys) as DbKey<R>
    }

    /**
     * 遍历每个字段
     * @param action 操作函数
     */
    public inline fun forEachColumn(action: (i: Int, col: T) -> Unit) {
        var i = 0
        for (item in columns)
            action(i++, item)
    }

    /**
     * 与另外一个主键 遍历字段
     *
     * @param other
     * @param action 操作函数
     */
    public inline fun <S> forEachColumnWith(other: DbKey<S>, action: (col1: T, col2: S, i:Int) -> Unit) {
        if (this.size != other.size)
            throw IllegalArgumentException("遍历2个主键时size不匹配: 第一个size为${this.size}, 第二个size为${other.size}")

        this.forEachColumn { i, col1 ->
            action(col1, other.getColumn(i), i)
        }
    }

    /**
     * Returns `true` if at least one element matches the given [predicate].
     *
     * @sample samples.collections.Collections.Aggregates.anyWithPredicate
     */
    public inline fun <S> anyColumnWith(o: DbKey<S>, predicate: (T, S) -> Boolean): Boolean {
        forEachColumnWith(o){ col1: T, col2: S, i:Int ->
            if (predicate(col1, col2))
                return true
        }
        return false
    }

    /**
     * Returns `true` if all elements match the given [predicate].
     *
     * @sample samples.collections.Collections.Aggregates.all
     */
    public inline fun <S> allColumnWith(o: DbKey<S>, predicate: (T, S) -> Boolean): Boolean {
        forEachColumnWith(o){ col1: T, col2: S, i:Int ->
            if (!predicate(col1, col2))
                return false
        }
        return true
    }

    /**
     * 访问字段
     * @param i
     * @return
     */
    public fun getColumn(i: Int): T{
        return columns[i]
    }

    /**
     * 检查元素值相等
     *    在 OrmQueryBuilder.setHasManyProp() 中用于匹配一对多的外键来设置关联属性
     */
    public override fun equals(o: Any?): Boolean {
        if(o == null)
            return false

        if(o !is DbKey<*>)
            return false

        return columns.contentEquals(o.columns)
    }

    public override fun hashCode(): Int {
        return columns.hashCode()
    }

    public override fun toString(): String{
        return columns.joinToString(", ", "DbKey[", "]")
    }

}

// 主键的字段名
typealias DbKeyNames = DbKey<String>

// 主键的字段值
internal typealias DbKeyValues = DbKey<Any?>

/*************************** 普通类扩展 ******************************/
/**
 * 检查Map是否全部包含主键字段
 *
 * @param names
 * @return
 */
internal inline fun Map<String, *>.containsAllKeys(names: DbKeyNames): Boolean{
    return names.columns.all {
        Map@this.containsKey(it)
    }
}

/*************************** DbKeyNames扩展 ******************************/
/**
 * 遍历字段名+字段值
 *
 * @param values
 * @param action 操作函数
 * @return
 */
internal inline fun DbKeyNames.forEachNameValue(values:Any?, action: (name: String, value: Any?, i:Int) -> Unit) {
    // 1 多值: DbKeyValue
    if(values is DbKey<*>) {
        forEachColumnWith(values, action)
        return
    }

    // 2 单值
    if(size != 1)
        throw IllegalArgumentException("遍历2个主键时size不匹配: 第一个size为${this.size}, 第二个是单值")

    action(single(), values, 0)
}

/**
 * 添加前缀/后缀
 *
 * @param prefix 前缀
 * @param postfix 后缀
 * @return
 */
internal inline fun DbKeyNames.wrap(prefix: CharSequence = "", postfix: CharSequence = ""): DbKeyNames {
    return this.map {
        "$prefix$it$postfix"
    }
}

/**
 * 字段是否为空
 * @return
 */
internal inline fun DbKeyNames.isAllEmpty(): Boolean{
    return columns.isEmpty() || columns.all {
        it.isEmpty()
    }
}

/*************************** IOrmMeta扩展 ******************************/
/**
 * 根据对象属性名，获得db字段名 -- 多个字段
 *    可根据实际需要在 model 类中重写
 *
 * @param props 对象属性名
 * @return db字段名
 */
internal inline fun IOrmMeta.props2Columns(props:DbKeyNames): DbKeyNames {
    return props.map {
        prop2Column(it)
    }
}

/**
 * 根据db字段名，获得对象属性名 -- 多个字段
 *
 * @param columns db字段名
 * @return 对象属性名
 */
inline fun IOrmMeta.columns2Props(columns:DbKeyNames): DbKeyNames {
    return columns.map {
        column2Prop(it)
    }
}

/*************************** IOrm扩展 ******************************/
/**
 * 获得对象字段
 *
 * @param names 字段名
 * @return
 */
internal inline fun IOrm.gets(names: DbKeyNames): DbKeyValues{
    return names.map {
        get<Any?>(it)
    }
}

/**
 * 设置对象字段
 *
 * @param columns 字段名
 * @param values 字段值
 */
internal inline fun IOrm.sets(columns: DbKeyNames, values: Any?){
    columns.forEachNameValue(values){ name, value, i ->
        set(name, value)
    }
}

/**
 * 收集某列的值
 *
 * @param names 列名
 * @return
 */
internal fun Collection<out IOrm>.collectColumn(names: DbKeyNames):DbKey<List<Any?>> {
    if (this.isEmpty())
        return DbKey.empty as DbKey<List<Any?>>

    return names.map { name ->
        this.collectColumn(name)
    }
}

/*************************** IDbQueryBuilder扩展 ******************************/
/**
 * where in
 *
 * @param   columns  column name or DbExpr
 * @param   values   column value
 * @return
 */
internal fun IDbQueryBuilder.whereIn(columns: DbKeyNames, values: DbKey<List<Any?>>): IDbQueryBuilder {
    // 每列有多值
    // 收集值不同的列
    val diffValuesIndexs = ArrayList<Int>()
    values.forEachColumn { i, value ->
        if(!value.isSame())
            diffValuesIndexs.add(i)
    }

    // TODO: 如果不同值的有多列, 则可以拼接 or 条件
    if(diffValuesIndexs.size > 1)
        throw IllegalArgumentException("DbKeyKt.whereIn()暂时只支持: 只有一列有不同值, 其他列每列有相同值")

    columns.forEachNameValue(values){ name, value, i ->
        val list = (value as List<*>)
        if(diffValuesIndexs.contains(i)) // 不同值的列用 where in
            IDbQueryBuilder@this.where(name, "IN", list.toSet())
        else // 相同值的列用 where =
            IDbQueryBuilder@this.where(name, list.first())
    }

    return this
}

/**
 * where =
 *
 * @param   columns  column name or DbExpr
 * @param   values   column value
 * @return
 */
internal fun IDbQueryBuilder.where(columns: DbKeyNames, values: Any?): IDbQueryBuilder {
    columns.forEachNameValue(values){ name, value, i ->
        IDbQueryBuilder@this.where(name, value)
    }
    return this
}

/**
 * Adds "ON ..." conditions for the last created JOIN statement.
 *
 * @param   cols1  column name or DbExpr
 * @param   cols2  column name or DbExpr
 * @return
 */
internal fun IDbQueryBuilder.on(cols1: DbKeyNames, cols2: DbKeyNames): IDbQueryBuilder {
    cols1.forEachColumnWith(cols2){ col1, col2, i ->
        IDbQueryBuilder@this.on(col1, "=", col2)
    }
    return this
}

/**
 * 设置更新的单个值, update时用
 *
 * @param columns
 * @param values
 * @return
 */
internal fun IDbQueryBuilder.set(columns:DbKeyNames, values:Any?): IDbQueryBuilder {
    columns.forEachNameValue(values){ name, value, i ->
        IDbQueryBuilder@this.set(name, value)
    }
    return this
}

/**
 * 设置插入的单行值, insert时用
 *   插入的值的数目必须登录插入的列的数目
 *
 * @param row
 * @return
 */
internal fun IDbQueryBuilder.insertValue(vararg row:Any?): IDbQueryBuilder {
    val vals = ArrayList<Any?>()
    row.forEach {
        if(it is DbKey<*>)
            vals.addAll(it.columns)
        else
            vals.add(it)
    }
    return this.value(*vals.toArray())
}

