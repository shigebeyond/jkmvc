package com.jkmvc.orm

import com.jkmvc.db.DbQueryBuilder
import com.jkmvc.orm.MetaData
import com.jkmvc.orm.MetaRelation
import com.jkmvc.orm.RelationType
import java.util.*

/**
 * 面向orm对象的sql构建器
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-16 下午8:02:28
 *
 */
class OrmQueryBuilder(protected val metadata: MetaData) : DbQueryBuilder(metadata.db, metadata.table) {

    /**
     * 联查表
     *
     * @param string name 关联关系名
     * @param array columns 字段名数组: array(column1, column2, alias => column3),
     * 													如 array("name", "age", "birt" => "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return OrmQueryBuilder
     */
    public fun with(name: String, vararg columns: String): OrmQueryBuilder {
        // select当前表字段
        if (this.selectColumns.isEmpty())
            this.select(metadata.table + ".*");

        // 获得关联关系
        val relation: MetaRelation? = metadata.getRelation(name)!!;
        if (relation != null) {
            // 根据关联关系联查表
            when (relation.type) {
            // belongsto: 查主表
                RelationType.BELONGS_TO ->
                    this.joinMaster(relation.metadata, relation.foreignKey, name);
            // hasxxx: 查从表
                else -> this.joinSlave(relation.metadata, relation.foreignKey, name);
            }
            // select关联表字段
            this.selectRelated(relation.metadata, name);
        }

        return this;
    }

    /**
     * 联查从表
     *     从表.外键 = 主表.主键
     *
     * @param MetaData slave 从类元数据
     * @param string foreignKey 外键
     * @param string tableAlias 表别名
     * @return OrmQueryBuilder
     */
    protected fun joinSlave(slave: MetaData, foreignKey: String, tableAlias: String): OrmQueryBuilder {
        // 联查从表
        val master: MetaData = metadata;
        val masterPk = master.table + "." + master.primaryKey;
        val slaveFk = tableAlias + "." + foreignKey;
        return this.join(tableAlias to slave.table, "LEFT").on(slaveFk, "=", masterPk) as OrmQueryBuilder; // 从表.外键 = 主表.主键
    }

    /**
     * 联查主表
     *     主表.主键 = 从表.外键
     *
     * @param MetaData master 主类元数据
     * @param string foreignKey 外键
     * @param string tableAlias 表别名
     * @return OrmQueryBuilder
     */
    protected fun joinMaster(master: MetaData, foreignKey: String, tableAlias: String): OrmQueryBuilder {
        // 联查从表
        val slave: MetaData = metadata;
        val masterPk = master.table + "." + master.primaryKey;
        val slaveFk = slave.table + "." + foreignKey;
        return this.join(master.table, "LEFT").on(masterPk, "=", slaveFk) as OrmQueryBuilder; // 主表.主键 = 从表.外键
    }

    /**
     * select关联表的字段
     *
     * @param MetaData related 关联类元数据
     * @param string tableAlias 表别名
     * @param array columns 查询的列
     */
    protected fun selectRelated(related: MetaData, tableAlias: String, columns: List<String>? = null): OrmQueryBuilder {
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

}
