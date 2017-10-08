package com.jkmvc.orm

import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.db.DbType
import com.jkmvc.db.IDbQueryBuilder
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

/**
 * 面向orm对象的sql构建器
 *    当前表与关联表都带别名
 *    当前表的别名=ormMeta.name
 *    关联表的别名=关联关系名
 *
 * @author shijianhang
 * @date 2016-10-16 下午8:02:28
 *
 */
class OrmQueryBuilder(protected val ormMeta: IOrmMeta /* orm元数据 */,
                      protected val convertValue: Boolean = false /* 查询时是否智能转换字段值 */,
                      protected val convertColumn: Boolean = false /* 查询时是否智能转换字段名 */
    ) : DbQueryBuilder(ormMeta.db, Pair(ormMeta.table, ormMeta.name)) {

    /**
     * 关联查询的记录，用于防止重复join同一个表
     */
    protected val joins:MutableList<String> = LinkedList()

    /**
     * 关联查询hasMany的关系，需单独处理，不在一个sql中联查，而是单独查询
     * <hasMany关系名, [子关系名+子关系字段]>
     */
    protected val joinMany:MutableMap<String, MutableList<Pair<String, List<String>?>>> = HashMap()

    /**
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        joins.clear()
        return super.clear()
    }

    /**
     * 获得记录转换器
     * @param clazz 要转换的类型
     * @return 转换函数
     */
    public override fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T) {
        // 只能是当前model类及其父类，不能是其他model类
        if(IOrm::class.java.isAssignableFrom(clazz.java) // 是model类
            && !clazz.java.isAssignableFrom(ormMeta.model.java)) // 不是当前model类及其父类
            throw UnsupportedOperationException("sql构建器将记录转为指定类型：只能指定 ${ormMeta.model} 类及其父类，实际指定 ${clazz}");

        return super.getRecordTranformer(clazz)
    }

    /********************************* with系列方法，用于实现关联对象查询 **************************************/
    /**
     * 联查多表
     *
     * @param names 关联关系名的数组
     * @return
     */
    public fun withs(names: List<String>): OrmQueryBuilder {
        for(name in names)
            with(name)
        return this
    }

    /**
     * 联查多表
     *
     * @param names 关联关系名的数组
     * @return
     */
    public fun withs(vararg names: String): OrmQueryBuilder {
        for(name in names)
            with(name)
        return this
    }

    /**
     * 联查表
     *
     * @param name 关联关系名
     * @param columns 字段名数组: Array(column1, column2, alias to column3),
     * 						如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    public fun with(name: String, columns: List<String>? = null): OrmQueryBuilder {
        // select当前表字段
        if (selectColumns.isEmpty())
            select(ormMeta.name + ".*");

        // join关联表
        ormMeta.joinRelated(this, name, columns)

        return this
    }

    /**
     * 联查joinMany关系的子关系
     *
     * @param name joinMany关系名
     * @param subname 子关系名
     * @param subcolumns 子关系字段名数组: Array(column1, column2, alias to column3),
     * 						如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    public fun withMany(name: String, subname: String? = null, subcolumns: List<String>? = null): OrmQueryBuilder {
        //数据结构：<hasMany关系名, [子关系名+子关系字段]>
        val subwiths = joinMany.getOrPut(name){
            LinkedList<Pair<String, List<String>?>>()
        }

        // 添加子关系
        if(subname != null)
            subwiths.add(Pair(subname, subcolumns))

        return this
    }

    /**
     * 联查从表
     *     从表.外键 = 主表.主键
     *
     * @param master 主类的元数据
     * @param relation 从类的关联关系
     * @param relationName 表别名
     * @return
     */
    public fun joinSlave(master: IOrmMeta, relation: IRelationMeta, relationName: String): OrmQueryBuilder {
        // 检查并添加关联查询记录
        if(joins.contains(relationName))
            return this;

        joins.add(relationName)

        // 准备条件
        val masterPk = master.name + "." + relation.primaryKey; // 主表.主键

        val slave = relation.ormMeta
        val slaveFk = relationName + "." + relation.foreignKey; // 从表.外键

        // 查从表
        return join(slave.table to relationName, "LEFT").on(slaveFk, "=", masterPk) as OrmQueryBuilder; // 从表.外键 = 主表.主键
    }

    /**
     * 联查主表
     *     主表.主键 = 从表.外键
     *
     * @param relation 从表关系
     * @param relationName 表别名
     * @return
     */
    public fun joinMaster(slave: IOrmMeta, relation: IRelationMeta, relationName: String): OrmQueryBuilder {
        // 检查并添加关联查询记录
        if(joins.contains(relationName))
            return this;

        joins.add(relationName)

        // 准备条件
        val slaveFk = slave.name + "." + relation.foreignKey; // 从表.外键

        val master: IOrmMeta = relation.ormMeta;
        val masterPk = relationName + "." + relation.primaryKey; // 主表.主键

        // 查主表
        return join(master.table to relationName, "LEFT").on(masterPk, "=", slaveFk) as OrmQueryBuilder; // 主表.主键 = 从表.外键
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 动态参数
     * @param transform 转换函数
     * @return 单个数据
     */
    public override fun <T:Any> find(vararg params: Any?, transform:(MutableMap<String, Any?>) -> T): T?{
        val result = super.find(*params, transform = transform);
        // 联查hasMany
        if(result is Orm){
            for((name, subwiths) in joinMany){
                // 联查hasMany的关系
                // 1 只联查一层 -- wrong
                //result.related(name, false)

                // 2 递归联查多层
                val relation = ormMeta.getRelation(name)!! // 获得关联关系
                val query = result.queryRelated(relation) // 构造关联查询
                for ((subname, subcolumns) in subwiths){ // 联查子关系
                    query.with(subname, subcolumns)
                }
                result[name] = query.findAll(transform = relation.recordTranformer)
            }
        }
        return result
    }

    /**
     * select关联表的字段
     *
     * @param relation 关联关系
     * @param relationName 表别名
     * @param columns 查询的列
     * @param path 之前的路径
     */
    public fun selectRelated(relation: IRelationMeta, relationName: String, columns: List<String>? = null, path: String = ""): OrmQueryBuilder {
        // 单独处理hasMany关系，不在一个sql中联查，而是单独查询
        if(relation.type == RelationType.HAS_MANY){
            return this;
        }

        // 默认查全部列
        val cols = if(columns == null)
                        relation.ormMeta.columns
                    else
                        columns

        // 构建列别名
        val select: MutableList<Pair<String, String>> = LinkedList<Pair<String, String>>();
        for (column in cols) {
            val columnAlias = path + ":" + column; // 列别名 = 表别名 : 列名，以便在设置orm对象字段值时，可以逐层设置关联对象的字段值
            val column = relationName + "." + column;
            select.add(column to columnAlias);
        }

        return this.select(select) as OrmQueryBuilder;
    }

    /********************************* 改写父类的方法：支持自动转换字段名（多级属性名 => 字段名）与字段值（字符串类型的字段值 => 准确类型的字段值），不改写join()，请使用with()来代替 **************************************/
    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   prop  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun andWhere(prop: String, op: String, value: Any?): IDbQueryBuilder {
        // 转换字段名：数据库中的字段名
        val column = if(convertColumn)
                        prop2Column(prop)
                    else
                        prop

        // 智能转换字段值: 准确类型的值
        val accurateValue = if(convertValue && (value is String || value is Array<*> || value is List<*>))
                            convertIntelligent(prop, value)
                        else
                            value
        return super.andWhere(column, op, accurateValue)
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   prop  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun orWhere(prop: String, op: String, value: Any?): IDbQueryBuilder {
        // 转换字段名：数据库中的字段名
        val column = if(convertColumn)
                        prop2Column(prop)
                    else
                        prop

        // 智能转换字段值: 准确类型的值
        val accurateValue = if(convertValue && (value is String || value is Array<*> || value is List<*>))
                                convertIntelligent(prop, value)
                            else
                                value

        return orWhere(column, op, accurateValue)
    }

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   prop     column name or array(column, alias) or object
     * @param   direction  direction of sorting
     * @return
     */
    public override fun orderBy(prop: String, direction: String?): IDbQueryBuilder {
        // 转换字段名：数据库中的字段名
        val column = if(convertColumn)
                        prop2Column(prop)
                    else
                        prop
        return super.orderBy(column, direction);
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   prop  column name
     * @return
     */
    public override fun groupBy(prop: String): IDbQueryBuilder {
        // 转换字段名：数据库中的字段名
        val column = if(convertColumn)
                        prop2Column(prop)
                    else
                        prop
        return super.groupBy(column)
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   prop  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun andHaving(prop: String, op: String, value: Any?): IDbQueryBuilder {
        // 转换字段名：数据库中的字段名
        val column = if(convertColumn)
                        prop2Column(prop)
                    else
                        prop

        // 智能转换字段值: 准确类型的值
        val accurateValue = if(convertValue && (value is String || value is Array<*> || value is List<*>))
                                convertIntelligent(prop, value)
                            else
                                value

        return super.andHaving(column, op, accurateValue)
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   prop  column name or array(column, alias) or object
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun orHaving(prop: String, op: String, value: Any?): IDbQueryBuilder {
        // 转换字段名：数据库中的字段名
        val column = if(convertColumn)
                        prop2Column(prop)
                    else
                        prop

        // 智能转换字段值: 准确类型的值
        val accurateValue = if(convertValue && (value is String || value is Array<*> || value is List<*>))
                                convertIntelligent(prop, value)
                            else
                                value

        return super.orHaving(column, op, accurateValue)
    }

    /**
     * 智能转换字段值
     *
     * @param prop 多级属性
     * @param value
     * @return
     */
    protected fun convertIntelligent(prop: String, value: Any): Any? {
        // 1 解析出：关联模型 + 字段名
        val (model, column) = getModelAndColumn(prop)

        // 2 由关联模型来转
        // 2.1 多值
        if(value is Array<*>){
            val arr = arrayOfNulls<Any?>(value.size) // 要明确Array<T>，否则无法使用set()方法
            for(i in 0..(value.size - 1)){
                arr[i] = model.convertIntelligent(column, value[i] as String)
            }
            return arr
        }
        if(value is List<*>){
            val list = value as MutableList<Any?>
            for(i in 0..(value.size - 1)){
                list[i] = model.convertIntelligent(column, value[i] as String)
            }
            return list
        }

        // 2.2 单值
        return model.convertIntelligent(column, value as String)
    }

    /**
     * 根据属性获得最后一层的关联模型与字段
     *
     * @param prop 多级属性
     * @return
     */
    protected fun getModelAndColumn(prop: String): Pair<IOrmMeta, String> {
        // 解析出：表名 + 字段名
        var table: String
        var column: String
        if (prop.contains('.')) {
            val arr = prop.split('.')
            table = arr[0]
            column = arr[1]
        } else {
            table = ormMeta.name
            column = prop
        }

        // 当前表
        if (table == ormMeta.name) {
            // 由当前模型来转
            return Pair(ormMeta, column)
        }

        // 关联表
        // 获得关联关系
        val relation = ormMeta.getRelation(table)!!
        // 由关联模型来转
        return Pair(relation.ormMeta, column)
    }

    /**
     * 根据对象属性名，获得db字段名
     *    可根据实际需要在 model 类中重写
     *
     * @param prop 对象属性名
     * @return db字段名
     */
    protected fun prop2Column(prop:String): String{
        // 处理关键字
        if(db.dbType == DbType.Oracle && prop == "rownum"){
            return prop
        }

        // 表+属性
        if(prop.contains('.')){
            val (table, prop2) = prop.split('.')
            return table + '.' + ormMeta.prop2Column(prop2)
        }

        // 纯属性
        return ormMeta.prop2Column(prop)
    }
}
