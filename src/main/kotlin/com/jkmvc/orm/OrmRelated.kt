package com.jkmvc.orm

import java.util.*

/**
 * ORM之关联对象操作
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
open abstract class OrmRelated(data: MutableMap<String, Any?> = LinkedHashMap<String, Any?>()) : OrmPersistent(data) {
    /**
     * 设置对象字段值
     *
     * @param  string column 字段名
     * @param  mixed  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        if (!hasColumn(column))
            throw OrmException("类 class 没有字段 column");

        // 设置关联对象
        val relation = metadata.getRelation(column);
        if (relation != null) {
            this[column] = value;
            // 如果关联的是主表，则更新从表的外键
            val (type, model, foreignKey) = relation;
            if (type == RelationType.BELONGS_TO)
                this[foreignKey] = (value as Orm).pk();
            return;
        }

        super.set(column, value);
    }

    /**
     * 获得对象字段
     *
     * @param   string column 字段名
     * @return  mixed
     */
    public override operator fun <T> get(name: String, defaultValue: Any?): T {
        // 获得关联对象
        if (metadata.hasRelation(name))
            return related(name);

        return super.get(name, defaultValue);
    }

    /**
     * 获得/设置原始的字段值
     *
     * @param array data
     * @return Orm|array
     */
    public fun original(data: Map<String, Any?>): Orm {
        for ((column, value) in data) {
            // 关联查询时，会设置关联表字段的列别名（列别名 = 表别名 : 列名），可以据此来设置关联对象的字段值
            if (!column.contains(":")) // 自身字段
            {
                this.data[column] = value;
            }

            if (value !== null) // 关联对象字段: 不处理null的值, 因为left join查询时, 关联对象可能没有匹配的行
            {
                val (name, column) = column.split(":");
                val obj = this.related(name, true); // 创建关联对象
                obj.data[column] = value;
            }
        }

        return this;
    }

    /**
     * 获得关联对象
     *
     * @param string name 关联对象名
     * @param boolean 是否创建新对象：在查询db后设置原始字段值data()时使用
     * @param array columns 字段名数组: array(column1, column2, alias => column3),
     * 													如 array("name", "age", "birt" => "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return Orm
     */
    public override fun related(name: String, newed: Boolean, vararg columns: String): Orm {
        // 已缓存
        if (this.data.contains(name))
            return this.data[name] as Orm;

        // 获得关联关系
        val relation: MetaRelation = metadata.getRelation(name)!!;

        // 创建新对象
        if (newed) {
            val item = relation.model.java.newInstance() as Orm;
            this.data[name] = item;
            return item;
        }

        // 根据关联关系来构建查询
        var obj: Orm;
        when (relation.type) {
            RelationType.BELONGS_TO -> // belongsto: 查主表
                obj = this.queryMaster(relation.metadata, relation.foreignKey).select(columns).find();
            RelationType.HAS_ONE -> // hasxxx: 查从表
                obj = this.querySlave(relation.metadata, relation.foreignKey).select(columns).find();
            else -> // hasxxx: 查从表
                obj = this.querySlave(relation.metadata, relation.foreignKey).select(columns).findAll();
        }

        this.data[name] = obj;
        return obj;
    }

    /**
     * 查询关联的从表
     *
     * @param string class 从类元数据
     * @param string foreignKey 外键
     * @return OrmQueryBuilder
     */
    protected fun querySlave(model: MetaData, foreignKey: String): OrmQueryBuilder {
        return model.queryBuilder().where(foreignKey, this.pk()) as OrmQueryBuilder; // 从表.外键 = 主表.主键
    }

    /**
     * 查询关联的主表
     *
     * @param string class 主类元数据
     * @param string foreignKey 外键
     * @return OrmQueryBuilder
     */
    protected fun queryMaster(model: MetaData, foreignKey: String): OrmQueryBuilder {
        return model.queryBuilder().where(metadata.primaryKey, foreignKey) as OrmQueryBuilder; // 主表.主键 = 从表.外键
    }
}
