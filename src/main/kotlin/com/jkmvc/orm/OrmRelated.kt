package com.jkmvc.orm

/**
 * ORM之关联对象操作
 *
 * @Package packagename
 * @category
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
open abstract class OrmRelated: OrmPersistent() {
    /**
     * 设置对象字段值
     *
     * @param  string column 字段名
     * @param  mixed  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        // 设置关联对象
        val relation = metadata.getRelation(column);
        if (relation != null) {
            data[column] = value;
            // 如果关联的是主表，则更新从表的外键
            val (type, model, foreignKey) = relation;
            if (type == RelationType.BELONGS_TO)
                this[foreignKey] = (value as Orm).pk; // 更新字段 super.set(foreignKey, value.pk);
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
            return related(name, false) as T;

        return super.get(name, defaultValue);
    }

    /**
     * 设置原始的字段值
     *
     * @param array data
     * @return Orm|array
     */
    public override fun original(orgn: Map<String, Any?>): IOrm {
        for ((column, value) in orgn) {
            // 关联查询时，会设置关联表字段的列别名（列别名 = 表别名 : 列名），可以据此来设置关联对象的字段值
            if (!column.contains(":")){ // 自身字段
                data[column] = value;
            } else if (value !== null) {// 关联对象字段: 不处理null的值, 因为left join查询时, 关联对象可能没有匹配的行
                val (name, column) = column.split(":");
                val obj:OrmRelated = related(name, true) as OrmRelated; // 创建关联对象
                obj.data[column] = value;
            }
        }

        loaded = true;
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
    public override fun related(name: String, newed: Boolean, vararg columns: String): Any? {
        // 已缓存
        if (data.contains(name))
            return data[name] as Orm;

        // 获得关联关系
        val relation: MetaRelation = metadata.getRelation(name)!!;

        var result: Any?;
        if (newed) {  // 创建新对象
            result = relation.model.java.newInstance();
        }else{  // 根据关联关系来构建查询
            val transform:(MutableMap<String, Any?>) -> IOrm = {
                val obj = relation.model.java.newInstance() as IOrm;
                obj.original(it)
            }
            when (relation.type) {
                RelationType.BELONGS_TO -> // belongsto: 查主表
                    result = queryMaster(relation).select(columns).find(transform);
                RelationType.HAS_ONE -> // hasxxx: 查从表
                    result = querySlave(relation).select(columns).find(transform);
                else -> // hasxxx: 查从表
                    result = querySlave(relation).select(columns).findAll(transform);
            }
        }

        data[name] = result;
        return result;
    }

    /**
     * 查询关联的从表
     *
     * @param MetaRelation relation 从表关系
     * @return OrmQueryBuilder
     */
    protected fun querySlave(relation:MetaRelation): OrmQueryBuilder {
        return relation.queryBuilder().where(relation.foreignKey, pk) as OrmQueryBuilder; // 从表.外键 = 主表.主键
    }

    /**
     * 查询关联的主表
     *
     * @param MetaRelation relation 从表关系
     * @return OrmQueryBuilder
     */
    protected fun queryMaster(relation:MetaRelation): OrmQueryBuilder {
        return relation.queryBuilder().where(metadata.primaryKey, this[relation.foreignKey]) as OrmQueryBuilder; // 主表.主键 = 从表.外键
    }
}
