package com.jkmvc.orm

import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.db.DbType
import com.jkmvc.db.IDbQueryBuilder
import java.util.*
import kotlin.reflect.KClass

/**
 * 面向orm对象的sql构建器
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

    /**
     * 联查表
     *
     * @param name 关联关系名
     * @param columns 字段名数组: Array(column1, column2, alias to column3),
     * 													如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    public fun with(name: String, columns: List<String>? = null): OrmQueryBuilder {
        // select当前表字段
        if (selectColumns.isEmpty())
            select(ormMeta.table + ".*");

        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 根据关联关系联查表
        when (relation.type) {
            // belongsto: 查主表
            RelationType.BELONGS_TO -> joinMaster(relation, name);
            // hasxxx: 查从表
            else -> joinSlave(relation, name);
        }
        // select关联表字段
        return selectRelated(relation.ormMeta, name, columns);
    }


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
     * 联查从表
     *     从表.外键 = 主表.主键
     *
     * @param relation 从类的关联关系
     * @param tableAlias 表别名
     * @return
     */
    protected fun joinSlave(relation: IRelationMeta, tableAlias: String): OrmQueryBuilder {
        // 准备条件
        val slave = relation.ormMeta
        val slaveFk = tableAlias + "." + relation.foreignKey; // 从表.外键

        val master: IOrmMeta = ormMeta;
        val masterPk = master.table + "." + master.primaryKey; // 主表.主键o

        // 查从表
        return join(slave.table to tableAlias, "LEFT").on(slaveFk, "=", masterPk) as OrmQueryBuilder; // 从表.外键 = 主表.主键
    }

    /**
     * 联查主表
     *     主表.主键 = 从表.外键
     *
     * @param relation 从表关系
     * @param tableAlias 表别名
     * @return
     */
    protected fun joinMaster(relation: IRelationMeta, tableAlias: String): OrmQueryBuilder {
        // 准备条件
        val master: IOrmMeta = relation.ormMeta;
        val masterPk = tableAlias + "." + master.primaryKey; // 主表.主键

        val slave: IOrmMeta = ormMeta;
        val slaveFk = slave.table + "." + relation.foreignKey; // 从表.外键

        // 查主表
        return join(master.table to tableAlias, "LEFT").on(masterPk, "=", slaveFk) as OrmQueryBuilder; // 主表.主键 = 从表.外键
    }

    /**
     * select关联表的字段
     *
     * @param related 主表关系
     * @param tableAlias 表别名
     * @param columns 查询的列
     */
    protected fun selectRelated(related: IOrmMeta, tableAlias: String, columns: List<String>? = null): OrmQueryBuilder {
        // 默认查询全部列
        var cols = columns
        if (cols === null)
            cols = related.columns;

        // 构建列别名
        val select: MutableList<Pair<String, String>> = LinkedList<Pair<String, String>>();
        for (column in cols) {
            val columnAlias = tableAlias + ":" + column; // 列别名 = 表别名 : 列名，以便在设置orm对象字段值时，可以逐层设置关联对象的字段值
            val column = tableAlias + "." + column;
            select.add(column to columnAlias);
        }

        return this.select(select) as OrmQueryBuilder;
    }

    /**
     * 改写where，支持自动转换字段名与字段值
     *
     * @param prop
     * @param op
     * @param value
     */
    public override fun andWhere(prop: String, op: String, value: Any?): IDbQueryBuilder {
        // 智能转换字段值
        val realValue = if(convertValue && value is String)
                            convertIntelligent(prop, value)
                        else
                            value
        // 转换字段名
        val realColumn = if(convertColumn)
                            prop2Column(prop)
                        else
                            prop
        return super.andWhere(realColumn, op, value)
    }

    /**
     * 智能转换字段值
     *
     * @param prop
     * @param value
     * @return
     */
    protected fun convertIntelligent(prop: String, value: String): Any? {
        if(prop.contains('.')){
            val (table, column) = prop.split('.')
            // 获得关联关系
            val relation = ormMeta.getRelation(table)!!
            // 由关联模型来转
            return relation.ormMeta.convertIntelligent(column, value)
        }
        // 由当前模型来转
        return ormMeta.convertIntelligent(prop, value)
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
