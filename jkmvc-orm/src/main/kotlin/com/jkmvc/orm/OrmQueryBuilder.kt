package com.jkmvc.orm

import com.jkmvc.db.DbQueryBuilder
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
class OrmQueryBuilder(protected val metadata: IMetaData) : DbQueryBuilder(metadata.db, metadata.table) {

    /**
     * 获得记录转换器
     * @param clazz 要转换的类型
     * @return 转换函数
     */
    public override fun <T:Any> getRecordTranformer(clazz: KClass<T>): ((MutableMap<String, Any?>) -> T) {
        // 只能是当前model类及其父类，不能是其他model类
        if(IOrm::class.java.isAssignableFrom(clazz.java) // 是model类
            && !clazz.java.isAssignableFrom(metadata.model.java)) // 不是当前model类及其父类
            throw UnsupportedOperationException("sql构建器将记录转为指定类型：只能指定 ${metadata.model} 类及其父类，实际指定 ${clazz}");

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
            select(metadata.table + ".*");

        // 获得关联关系
        val relation = metadata.getRelation(name)!!;
        // 根据关联关系联查表
        when (relation.type) {
            // belongsto: 查主表
            RelationType.BELONGS_TO -> joinMaster(relation, name);
            // hasxxx: 查从表
            else -> joinSlave(relation, name);
        }
        // select关联表字段
        return selectRelated(relation.metadata, name, columns);
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
     * 联查从表
     *     从表.外键 = 主表.主键
     *
     * @param relation 从类的关联关系
     * @param tableAlias 表别名
     * @return
     */
    protected fun joinSlave(relation: IMetaRelation, tableAlias: String): OrmQueryBuilder {
        // 准备条件
        val slave = relation.metadata
        val slaveFk = tableAlias + "." + relation.foreignKey; // 从表.外键

        val master: IMetaData = metadata;
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
    protected fun joinMaster(relation: IMetaRelation, tableAlias: String): OrmQueryBuilder {
        // 准备条件
        val master: IMetaData = relation.metadata;
        val masterPk = tableAlias + "." + master.primaryKey; // 主表.主键

        val slave: IMetaData = metadata;
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
    protected fun selectRelated(related: IMetaData, tableAlias: String, columns: List<String>? = null): OrmQueryBuilder {
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

    public override fun andWhere(column: String, op: String, value: Any?): IDbQueryBuilder {
        return super.andWhere(metadata.prop2Column(column), op, value)
    }
}
