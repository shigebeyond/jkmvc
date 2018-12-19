package com.jkmvc.orm

import com.jkmvc.db.IDbQueryBuilder

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
data class DbKey<T>(val columns: Array<T>) {

    companion object {
        /**
         * 空主键
         */
        internal val empty:DbKey<*> = DbKey<Any>(emptyArray())
    }

    // wrong: 主构造函数签名相同冲突
    //public constructor(vararg cols:T):this(cols)

    // 逐个实现1个参数/2个参数/3个参数的构造函数
    public constructor(a: T):this(toArray(a))

    public constructor(a: T, b:T):this(toArray(a, b))

    public constructor(a: T, b:T, c:T):this(toArray(a, b, c))

    /**
     * 字段个数
     */
    public val size = columns.size

    /**
     * 第一个字段
     */
    public fun first() = columns.first()

    /**
     * 遍历并生成新的主键
     *
     * @param transform 字段转换函数
     * @return
     */
    public inline fun <R> map(transform: (T) -> R): DbKey<R> {
        val newKeys = columns.clone() as Array<R>
        forEachColumn { i, v ->
            newKeys[i] = transform(columns[i])
        }
        return DbKey<R>(newKeys)
    }

    /**
     * 与另外一个主键 遍历并生成新的主键
     *
     * @param transform 字段转换函数
     * @return
     */
    public inline fun <S, R> mapWith(other: DbKey<S>, transform: (T, S) -> R): DbKey<R> {
        val newKeys = columns.clone() as Array<R>
        forEachColumnWith(other){ col1, col2, i ->
            newKeys[i] = transform(col1, col2)
        }
        return DbKey<R>(newKeys)
    }

    /**
     * 遍历每个字段
     * @param action 操作函数
     */
    public inline fun forEachColumn(action: (i: Int, col: T) -> Unit): Unit {
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
     * 访问字段
     * @param i
     * @return
     */
    public fun getColumn(i: Int): T{
        return columns[i]
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
 * 非空参数转为array, 仅用于在 DbKey/Orm 的构造函数中转参数
 * @param params
 * @return
 */
internal inline fun <S> toArray(vararg params:S): Array<S> {
    return params as Array<S>
}

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
    if(columns.size != 1)
        throw IllegalArgumentException("遍历2个主键时size不匹配: 第一个size为${this.size}, 第二个是单值")

    action(columns.first(), values, 0)
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

/*************************** DbKeyValues扩展 ******************************/
/**
 * 检查值是否为null -- 复合主键中的字段值不能为空
 * @return
 */
internal inline fun DbKeyValues.isAnyNull(): Boolean {
    return columns.isEmpty() || columns.all {
        it == null
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

/**
 * 检查指定外键值是否为空
 *
 * @param fks 外键值
 * @return
 */
internal fun IRelationMeta.isForeighKeysAllEmpty(fks: DbKeyValues): Boolean{
    return fks.columns.all {
        IRelationMeta@this.isForeighKeyEmpty(it)
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

    return names.map {
        this.collectColumn(it)
    }
}

/*************************** IDbQueryBuilder扩展 ******************************/
/**
 * Alias of andWhere()
 *
 * @param   columns  column name or DbExpr
 * @param   op      logic operator
 * @param   values   column value
 * @return
 */
fun IDbQueryBuilder.where(columns: DbKeyNames, op: String, values: Any?): IDbQueryBuilder {
    return andWhere(columns, op, values);
}

/**
 * Alias of andWhere()
 *
 * @param   columns  column name or DbExpr
 * @param   values   column value
 * @return
 */
fun IDbQueryBuilder.where(columns: DbKeyNames, values: Any?): IDbQueryBuilder {
    columns.forEachNameValue(values) { name, value, i ->
        IDbQueryBuilder@this.where(name, value)
    }
    return this
}

/**
 * Creates a new "AND WHERE" condition for the query.
 *
 * @param   columns  column name or DbExpr
 * @param   op      logic operator
 * @param   values   column value
 * @return
 */
fun IDbQueryBuilder.andWhere(columns: DbKeyNames, op: String, values: Any?): IDbQueryBuilder{
    columns.forEachNameValue(values){ name, value, i ->
        IDbQueryBuilder@this.andWhere(name, op, value)
    }
    return this
}

/**
 * Adds "ON ..." conditions for the last created JOIN statement.
 *
 * @param   cols1  column name or DbExpr
 * @param   op  logic operator
 * @param   cols2  column name or DbExpr
 * @return
 */
fun IDbQueryBuilder.on(cols1: DbKeyNames, op: String, cols2: DbKeyNames): IDbQueryBuilder{
    cols1.forEachColumnWith(cols2){ col1, col2, i ->
        IDbQueryBuilder@this.on(col1, op, col2)
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
fun IDbQueryBuilder.set(columns:DbKeyNames, values:Any?):IDbQueryBuilder{
    columns.forEachNameValue(values){ name, value, i ->
        IDbQueryBuilder@this.set(name, value)
    }
    return this
}

